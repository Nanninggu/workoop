<template>
  <transition name="chat-slide">
    <div v-if="open" class="chat-panel">
      <!-- 헤더 -->
      <div class="chat-header">
        <div class="chat-header-title">
          <MessageSquare :size="14" />
          <span>팀 채팅</span>
          <span v-if="orgName" class="chat-org">{{ orgName }}</span>
        </div>
        <button class="chat-close-btn" @click="$emit('close')" title="닫기 (C)">
          <X :size="15" />
        </button>
      </div>

      <!-- 메시지 목록 -->
      <div class="chat-messages" ref="listRef">
        <div v-if="store.loading" class="chat-state">
          <Loader2 :size="18" class="spin" />
          <span>불러오는 중...</span>
        </div>

        <template v-else>
          <div v-if="(aiMode ? aiMessages.length === 0 : store.messages.length === 0)" class="chat-state">
            <MessageSquare :size="28" style="opacity:0.15" />
            <p>첫 메시지를 보내보세요</p>
          </div>

          <template v-else>
            <!-- 날짜 구분선 + 메시지 그룹 -->
            <template v-for="(group, gi) in groupedMessages" :key="gi">
              <div class="chat-date-divider">
                <span>{{ group.date }}</span>
              </div>
              <template v-for="(msg, mi) in group.messages" :key="msg.id ?? ('ai-' + mi)">
                <!-- AI 메시지 -->
                <div v-if="msg.isAi" class="chat-msg ai-msg">
                  <div class="chat-avatar ai-avatar">
                    <Bot :size="13" />
                  </div>
                  <div class="chat-bubble-wrap">
                    <span class="chat-name">Workoop AI</span>
                    <div class="chat-bubble ai-bubble">{{ msg.content }}</div>
                    <span class="chat-time">{{ formatTime(msg.createdAt) }}</span>
                  </div>
                </div>

                <!-- 일반 메시지 -->
                <div
                  v-else
                  class="chat-msg"
                  :class="{
                    mine: msg.userId === myId,
                    'cont': mi > 0 && !group.messages[mi-1].isAi && group.messages[mi-1].userId === msg.userId
                  }"
                >
                  <div
                    v-if="msg.userId !== myId"
                    class="chat-avatar"
                    :class="{ invisible: mi > 0 && !group.messages[mi-1].isAi && group.messages[mi-1].userId === msg.userId }"
                  >
                    {{ (msg.userName || '?').charAt(0).toUpperCase() }}
                  </div>

                  <div class="chat-bubble-wrap">
                    <span
                      v-if="msg.userId !== myId && (mi === 0 || group.messages[mi-1].isAi || group.messages[mi-1].userId !== msg.userId)"
                      class="chat-name"
                    >{{ msg.userName }}</span>
                    <div class="chat-bubble">{{ msg.content }}</div>
                    <span
                      v-if="mi === group.messages.length - 1 || group.messages[mi+1].isAi || group.messages[mi+1].userId !== msg.userId"
                      class="chat-time"
                    >{{ formatTime(msg.createdAt) }}</span>
                  </div>
                </div>
              </template>
            </template>
          </template>
        </template>
      </div>

      <!-- AI 응답 대기 중 -->
      <div v-if="aiLoading" class="ai-thinking">
        <Loader2 :size="13" class="spin" />
        <span>AI 응답 생성 중...</span>
      </div>

      <!-- 입력창 -->
      <div class="chat-input-area" :class="{ 'ai-mode': aiMode }">
        <button
          class="mode-toggle-btn"
          :class="{ 'is-ai': aiMode }"
          @click="toggleMode"
          :title="aiMode ? '일반 채팅으로 전환' : 'AI 질문 모드로 전환'"
        >
          <transition name="mode-flip" mode="out-in">
            <Bot v-if="aiMode" :size="14" key="ai" />
            <MessageSquare v-else :size="14" key="chat" />
          </transition>
        </button>
        <textarea
          ref="inputRef"
          v-model="draft"
          class="chat-input"
          :placeholder="aiMode ? 'AI에게 질문하세요...' : '메시지 입력...'"
          rows="1"
          @keydown.enter.exact.prevent="handleEnter"
          @input="autoResize"
        />
        <button class="chat-send-btn" @click="submit" :disabled="!draft.trim() || aiLoading">
          <Send :size="14" />
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, nextTick, computed  } from 'vue'
import { MessageSquare, X, Send, Loader2, Bot } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/store/chatStore'
import { useAuthStore } from '@/store/authStore'
import { useOrgStore } from '@/store/orgStore'
import { chatApi } from '@/api/chatApi'

const props = defineProps({ open: Boolean })
defineEmits(['close'])

const store    = useChatStore()
const auth     = useAuthStore()
const orgStore = useOrgStore()

const myId    = auth.user?.id
const orgName = orgStore.currentOrg?.name

const draft        = ref('')
const listRef      = ref()
const inputRef     = ref()
const aiLoading    = ref(false)
const aiMode       = ref(false)
const aiMessages   = ref([])
const isSubmitting = ref(false)

function toggleMode() {
  aiMode.value = !aiMode.value
  nextTick(() => inputRef.value?.focus())
}

function formatDate(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const today = new Date()
  const isToday =
    d.getFullYear() === today.getFullYear() &&
    d.getMonth()    === today.getMonth() &&
    d.getDate()     === today.getDate()
  if (isToday) return '오늘'
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

const groupedMessages = computed(() => {
  const groups = []
  let lastDate = null
  let currentGroup = null

  // AI 모드: AI 메시지만, 일반 모드: 팀 채팅 메시지만
  const allMessages = aiMode.value
    ? [...aiMessages.value]
    : [...store.messages]

  allMessages
    .slice()
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    .forEach(msg => {
      const dateLabel = formatDate(msg.createdAt)
      if (dateLabel !== lastDate) {
        lastDate = dateLabel
        currentGroup = { date: dateLabel, messages: [] }
        groups.push(currentGroup)
      }
      currentGroup.messages.push(msg)
    })
  return groups
})

function handleEnter(e) {
  if (e.isComposing || e.keyCode === 229) return
  submit()
}

async function submit() {
  if (isSubmitting.value) return
  const content = draft.value.trim()
  if (!content) return
  if (!orgStore.currentOrg) {
    ElMessage.warning('조직에 가입 후 채팅할 수 있습니다. 온보딩을 완료해 주세요.')
    return
  }

  isSubmitting.value = true
  draft.value = ''
  nextTick(() => resetHeight())

  try {
    if (aiMode.value) {
      await askAi(content)
    } else {
      await store.sendMessage(orgStore.currentOrg.id, content)
    }
  } catch (err) {
    console.error('[Chat] submit 오류', err)
  } finally {
    isSubmitting.value = false
  }
}

async function askAi(question) {
  // 사용자 질문 먼저 표시
  aiMessages.value.push({
    isAi: false,
    userId: myId,
    userName: '나',
    content: question,
    createdAt: new Date().toISOString(),
  })
  aiLoading.value = true
  scrollBottom()
  try {
    const res = await chatApi.askAi(
      orgStore.currentOrg.id,
      orgStore.currentOrg.name,
      question
    )
    const answer = res?.data?.answer ?? res?.answer ?? '응답을 받지 못했습니다.'
    aiMessages.value.push({
      isAi: true,
      content: answer,
      createdAt: new Date().toISOString(),
    })
  } catch(e) {
    console.error('[Chat] AI 응답 오류', e)
    aiMessages.value.push({
      isAi: true,
      content: '⚠️ AI 응답을 가져오는 데 실패했습니다: ' + (e.message || '알 수 없는 오류'),
      createdAt: new Date().toISOString(),
    })
  } finally {
    aiLoading.value = false
    scrollBottom()
  }
}


function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function resetHeight() {
  if (inputRef.value) inputRef.value.style.height = 'auto'
}

function scrollBottom() {
  nextTick(() => {
    if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
  })
}

watch(() => store.messages.length, scrollBottom)

async function loadAiHistory() {
  try {
    const res = await chatApi.getAiHistory()
    const history = res.data ?? []
    aiMessages.value = history.map(h => ({
      id: h.id,
      isAi: h.role === 'assistant',
      userId: h.role === 'user' ? myId : null,
      userName: h.role === 'user' ? '나' : null,
      content: h.content,
      createdAt: h.createdAt,
    }))
  } catch (e) {
    console.warn('[AI History] 로드 실패:', e.message)
  }
}

watch(() => props.open, async (val) => {
  if (val && orgStore.currentOrg) {
    await Promise.all([
      store.fetchHistory(orgStore.currentOrg.id),
      loadAiHistory(),
    ])
    store.markRead()
    scrollBottom()
    nextTick(() => inputRef.value?.focus())
  }
})
</script>

<style scoped>
.chat-panel {
  position: fixed;
  right: 0;
  top: 0;
  width: 300px;
  height: 100vh;
  background: #16181C;
  border-left: 1px solid rgba(255,255,255,0.06);
  box-shadow: -8px 0 32px rgba(0,0,0,0.4);
  z-index: 150;
  display: flex;
  flex-direction: column;
}

/* 슬라이드 */
.chat-slide-enter-active {
  transition: opacity 0.22s ease, transform 0.28s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.chat-slide-leave-active {
  transition: opacity 0.16s ease, transform 0.2s cubic-bezier(0.4, 0, 1, 1);
}
.chat-slide-enter-from { opacity: 0; transform: translateX(100%); }
.chat-slide-leave-to  { opacity: 0; transform: translateX(100%); }

/* 헤더 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
  background: #1A1C21;
}
.chat-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94A3B8;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.chat-org {
  background: rgba(99,102,241,0.18);
  color: #818CF8;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 99px;
  letter-spacing: 0;
  text-transform: none;
}
.chat-close-btn {
  width: 24px; height: 24px;
  border: none;
  background: rgba(255,255,255,0.05);
  border-radius: 5px;
  color: #475569;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.chat-close-btn:hover { background: rgba(255,255,255,0.09); color: #94A3B8; }

/* 메시지 영역 — 아래부터 쌓이게 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end; /* ← 핵심: 메시지를 아래 정렬 */
  gap: 2px;
  min-height: 0;
}
.chat-messages::-webkit-scrollbar { width: 3px; }
.chat-messages::-webkit-scrollbar-track { background: transparent; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.07); border-radius: 3px; }

/* 로딩/빈 상태 */
.chat-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #3A4558;
  font-size: 0.8rem;
  padding: 32px 0;
  flex: 1;
}
.chat-state p { margin: 0; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 날짜 구분선 */
.chat-date-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 10px 4px 8px;
}
.chat-date-divider::before,
.chat-date-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(255,255,255,0.06);
}
.chat-date-divider span {
  font-size: 0.67rem;
  color: #3A4558;
  font-weight: 600;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

/* 메시지 */
.chat-msg {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  margin-top: 8px;
}
.chat-msg.cont { margin-top: 2px; }
.chat-msg.mine { flex-direction: row-reverse; }

.chat-avatar {
  width: 26px; height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5B5BD6, #7C3AED);
  color: white;
  font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  align-self: flex-end;
}
.chat-avatar.invisible { visibility: hidden; }

.chat-bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-width: 210px;
}
.chat-msg.mine .chat-bubble-wrap { align-items: flex-end; }

.chat-name {
  font-size: 0.68rem;
  color: #4A5568;
  padding-left: 4px;
  font-weight: 500;
}

.chat-bubble {
  background: rgba(255,255,255,0.08);
  color: #C8D3E0;
  padding: 7px 11px;
  border-radius: 14px 14px 14px 4px;
  font-size: 0.855rem;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.chat-msg.mine .chat-bubble {
  background: #3C5CDD;
  color: #EEF2FF;
  border-radius: 14px 14px 4px 14px;
}

.chat-time {
  font-size: 0.62rem;
  color: #3A4558;
  padding: 0 4px;
}

/* 입력창 */
.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  padding: 8px 10px 10px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
  background: #1A1C21;
}
.chat-input {
  flex: 1;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.09);
  border-radius: 10px;
  color: #C8D3E0;
  font-size: 0.855rem;
  font-family: inherit;
  line-height: 1.5;
  padding: 7px 10px;
  resize: none;
  outline: none;
  min-height: 34px;
  max-height: 120px;
  overflow-y: auto;
  transition: border-color 0.15s, background 0.15s;
}
.chat-input:focus {
  border-color: rgba(92,92,214,0.5);
  background: rgba(255,255,255,0.07);
}
.chat-input::placeholder {
  color: #4A5568;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-send-btn {
  width: 32px; height: 32px;
  border: none;
  background: #3C5CDD;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s, opacity 0.15s, transform 0.1s;
}
.chat-send-btn:hover:not(:disabled) { background: #4A6CF5; transform: scale(1.05); }
.chat-send-btn:active:not(:disabled) { transform: scale(0.95); }
.chat-send-btn:disabled { opacity: 0.25; cursor: default; }

/* 모드 토글 버튼 */
.mode-toggle-btn {
  width: 32px; height: 32px;
  flex-shrink: 0;
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: #4A5568;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.18s;
}
.mode-toggle-btn:hover {
  background: rgba(255,255,255,0.09);
  color: #94A3B8;
}
.mode-toggle-btn.is-ai {
  border-color: rgba(139,92,246,0.45);
  background: rgba(139,92,246,0.15);
  color: #A78BFA;
}
.mode-toggle-btn.is-ai:hover {
  background: rgba(139,92,246,0.25);
}

/* 모드 전환 아이콘 애니메이션 */
.mode-flip-enter-active,
.mode-flip-leave-active { transition: opacity 0.12s, transform 0.12s; }
.mode-flip-enter-from   { opacity: 0; transform: scale(0.6) rotate(-15deg); }
.mode-flip-leave-to     { opacity: 0; transform: scale(0.6) rotate(15deg); }

/* AI 메시지 버블 */
.ai-bubble {
  background: rgba(139,92,246,0.12) !important;
  color: #C4B5FD !important;
  border: 1px solid rgba(139,92,246,0.2);
  border-radius: 14px 14px 14px 4px !important;
  white-space: pre-wrap;
}
.ai-avatar {
  background: linear-gradient(135deg, #7C3AED, #4F46E5) !important;
  display: flex; align-items: center; justify-content: center;
}
.ai-msg .chat-name { color: #8B5CF6; }

/* AI 생각 중 표시 */
.ai-thinking {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  font-size: 0.75rem; color: #8B5CF6;
  background: rgba(139,92,246,0.06);
  border-top: 1px solid rgba(139,92,246,0.1);
}

/* AI 입력 모드 */
.chat-input-area.ai-mode {
  border-top-color: rgba(139,92,246,0.25);
  background: rgba(139,92,246,0.04);
}

.chat-input-area.ai-mode .chat-input {
  border-color: rgba(139,92,246,0.35);
}
.chat-input-area.ai-mode .chat-input:focus {
  border-color: rgba(139,92,246,0.6);
  background: rgba(139,92,246,0.06);
}
.chat-input-area.ai-mode .chat-send-btn {
  background: linear-gradient(135deg, #7C3AED, #4F46E5);
}

</style>
