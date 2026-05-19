<template>
  <div class="team-scrum-panel">

    <!-- 헤더: 날짜 + 스탠드업 시작 버튼 -->
    <div class="tsp-header">
      <div class="tsp-date">
        <CalendarDays :size="15" />
        {{ formattedDate }} 팀 스크럼
      </div>
      <div class="tsp-controls">
        <button class="btn-refresh" @click="load" :disabled="loading">
          <RefreshCw :size="14" :class="{ spin: loading }" />
        </button>
        <button
          v-if="!timerRunning"
          class="btn-standup"
          @click="startStandup"
          :disabled="members.length === 0"
        >
          <Play :size="14" /> 스탠드업 시작 (15분)
        </button>
        <div v-else class="standup-bar">
          <div class="standup-time" :class="{ urgent: timerSec < 60 }">
            {{ timerLabel }}
          </div>
          <div class="standup-speaker">
            <span class="speaker-label">발표자</span>
            <span class="speaker-name">{{ currentSpeaker?.userName || '-' }}</span>
          </div>
          <button class="btn-next" @click="nextSpeaker" :disabled="speakerIdx >= members.length - 1">
            다음 <ChevronRight :size="13" />
          </button>
          <button class="btn-stop-timer" @click="stopStandup">종료</button>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-if="!loading && members.length === 0" class="tsp-empty">
      <Users :size="40" class="tsp-empty-icon" />
      <p>오늘 스크럼을 등록한 팀원이 없습니다.</p>
      <p class="tsp-empty-hint">내 스크럼 탭에서 오늘의 상태를 저장하면 여기에 표시됩니다.</p>
    </div>

    <!-- 팀원 카드 그리드 -->
    <div v-else class="tsp-grid">
      <div
        v-for="(m, idx) in members"
        :key="m.userId"
        class="member-scrum-card"
        :class="{
          'card-speaker': timerRunning && speakerIdx === idx,
          'card-blocker': m.blockerSeverity === 'HIGH'
        }"
      >
        <!-- 카드 헤더 -->
        <div class="msc-header">
          <div class="msc-avatar">{{ initial(m.userName) }}</div>
          <div class="msc-name-wrap">
            <div class="msc-name">{{ m.userName }}</div>
            <div class="msc-sub">
              <span v-if="timerRunning && speakerIdx === idx" class="speaking-badge">🎙 발표 중</span>
            </div>
          </div>
          <!-- 에너지 레벨 -->
          <div class="msc-energy" :style="{ color: energyColor(m.energy) }" :title="`에너지 ${m.energy}/5`">
            <span v-for="i in 5" :key="i" class="energy-dot" :class="{ filled: i <= m.energy }">●</span>
          </div>
        </div>

        <!-- 집중 목표 -->
        <div v-if="m.focus" class="msc-focus">
          <Target :size="12" /> {{ m.focus }}
        </div>

        <!-- 태스크 진행률 -->
        <div class="msc-tasks" v-if="m.tasks.length > 0">
          <div class="msc-task-bar-wrap">
            <div
              class="msc-task-bar-fill"
              :style="{ width: taskRate(m) + '%', background: rateColor(taskRate(m)) }"
            ></div>
          </div>
          <span class="msc-task-count">
            {{ m.doneTasks }}/{{ m.tasks.length }} 완료
          </span>
        </div>
        <div v-else class="msc-no-tasks">태스크 없음</div>

        <!-- 블로커 -->
        <div v-if="m.blocker" class="msc-blocker" :class="`sev-${m.blockerSeverity.toLowerCase()}`">
          <span class="blocker-icon">{{ m.blockerSeverity === 'HIGH' ? '🔴' : m.blockerSeverity === 'MEDIUM' ? '🟡' : '🟢' }}</span>
          <span class="blocker-text">{{ m.blocker }}</span>
        </div>

        <!-- 오늘 할 일 목록 (펼치기) -->
        <div class="msc-task-list" v-if="expanded[idx]">
          <div v-for="t in m.tasks" :key="t.id || t.title || t.text" class="msc-task-item">
            <span class="task-dot" :class="{ done: t.done }">{{ t.done ? '✓' : '○' }}</span>
            <span :class="{ 'task-done': t.done }">{{ t.title || t.text }}</span>
          </div>
        </div>
        <button class="btn-expand" @click="toggleExpand(idx)">
          {{ expanded[idx] ? '접기 ▲' : `할 일 보기 (${m.tasks.length}개) ▼` }}
        </button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useOrgStore } from '@/store/orgStore'
import { useWsStore } from '@/store/wsStore'
import { scrumApi } from '@/api/scrumApi'
import { orgApi } from '@/api/orgApi'
import { CalendarDays, RefreshCw, Play, ChevronRight, Users, Target } from 'lucide-vue-next'
import dayjs from 'dayjs'

const orgStore = useOrgStore()
const wsStore  = useWsStore()

const members  = ref([])  // 팀원별 스크럼 데이터 (가공됨)
const loading  = ref(false)
const expanded = ref({})

// 스탠드업 타이머
const STANDUP_SEC  = 15 * 60
const timerRunning = ref(false)
const timerSec     = ref(STANDUP_SEC)
const speakerIdx   = ref(0)
let   _timer       = null

const formattedDate  = computed(() => dayjs().format('YYYY년 MM월 DD일 dddd'))
const currentSpeaker = computed(() => members.value[speakerIdx.value] || null)

const timerLabel = computed(() => {
  const m = Math.floor(timerSec.value / 60).toString().padStart(2, '0')
  const s = (timerSec.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

async function load() {
  if (!orgStore.currentOrg) return
  loading.value = true
  try {
    // 팀 스크럼 데이터 + 멤버 목록 병렬 로드
    const [scrumRes, memberRes] = await Promise.all([
      scrumApi.team(orgStore.currentOrg.id, dayjs().format('YYYY-MM-DD')),
      orgApi.members(orgStore.currentOrg.id),
    ])

    const scrumMap = {}
    ;(scrumRes.data || []).forEach(s => { scrumMap[s.userId] = s })

    // 모든 멤버를 표시 (스크럼 없으면 빈 상태)
    members.value = (memberRes.data || []).map(m => {
      const scrum = scrumMap[m.userId] || {}
      let tasks = []
      try { tasks = JSON.parse(scrum.tasksJson || '[]') } catch {}
      return {
        userId:          m.userId,
        userName:        m.userName,
        energy:          scrum.energy || 0,
        focus:           scrum.focus || '',
        blocker:         scrum.blocker || '',
        blockerSeverity: scrum.blockerSeverity || 'LOW',
        tasks,
        doneTasks:       tasks.filter(t => t.done).length,
        hasScrum:        !!scrumMap[m.userId],
      }
    })
  } finally {
    loading.value = false
  }
}

function taskRate(m) {
  if (!m.tasks.length) return 0
  return Math.round((m.doneTasks / m.tasks.length) * 100)
}

function rateColor(r) {
  if (r >= 70) return '#10B981'
  if (r >= 40) return '#F59E0B'
  return '#EF4444'
}

function energyColor(e) {
  if (e >= 4) return '#10B981'
  if (e === 3) return '#F59E0B'
  if (e >= 1) return '#EF4444'
  return '#CBD5E1'
}

function initial(name) { return (name || '?').charAt(0).toUpperCase() }

function toggleExpand(idx) {
  expanded.value[idx] = !expanded.value[idx]
}

function startStandup() {
  timerRunning.value = true
  timerSec.value     = STANDUP_SEC
  speakerIdx.value   = 0
  _timer = setInterval(() => {
    if (timerSec.value > 0) {
      timerSec.value--
    } else {
      stopStandup()
    }
  }, 1000)
}

function nextSpeaker() {
  if (speakerIdx.value < members.value.length - 1) {
    speakerIdx.value++
  }
}

function stopStandup() {
  timerRunning.value = false
  clearInterval(_timer)
  _timer = null
}

let _wsScrumUnsub = null

onMounted(async () => {
  await load()
  // 다른 팀원이 스크럼을 제출하면 목록 자동 갱신
  _wsScrumUnsub = wsStore.on('SCRUM_UPDATED', () => load())
})

onUnmounted(() => {
  stopStandup()
  _wsScrumUnsub?.()
})
</script>

<style scoped>
.team-scrum-panel { padding: 0; }

/* 헤더 */
.tsp-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
}
.tsp-date {
  display: flex; align-items: center; gap: 6px;
  font-size: 0.9rem; font-weight: 700; color: var(--text-primary);
}
.tsp-controls { display: flex; align-items: center; gap: 8px; }

.btn-refresh {
  width: 32px; height: 32px; border-radius: var(--radius-md);
  background: var(--bg-hover); border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary);
}
.btn-refresh:hover { background: var(--border-color); }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.btn-standup {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.85rem;
  font-weight: 600; cursor: pointer;
}
.btn-standup:hover:not(:disabled) { opacity: 0.88; }
.btn-standup:disabled { opacity: 0.5; cursor: not-allowed; }

/* 스탠드업 바 */
.standup-bar {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-sidebar); padding: 8px 14px; border-radius: var(--radius-md);
}
.standup-time {
  font-size: 1.1rem; font-weight: 800; color: #93C5FD;
  font-variant-numeric: tabular-nums; min-width: 52px; font-family: 'JetBrains Mono', monospace;
}
.standup-time.urgent { color: var(--color-danger); animation: blink 1s ease-in-out infinite; }
@keyframes blink { 50% { opacity: 0.5; } }
.standup-speaker { display: flex; align-items: center; gap: 6px; }
.speaker-label { font-size: 0.72rem; color: rgba(255,255,255,0.4); }
.speaker-name { font-size: 0.875rem; font-weight: 700; color: white; }
.btn-next {
  display: flex; align-items: center; gap: 3px;
  padding: 5px 10px; background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.75);
  border: none; border-radius: var(--radius-sm); font-size: 0.78rem; cursor: pointer;
}
.btn-next:hover:not(:disabled) { background: rgba(255,255,255,0.2); }
.btn-next:disabled { opacity: 0.4; }
.btn-stop-timer {
  padding: 5px 10px; background: rgba(239,68,68,0.2); color: #FCA5A5;
  border: none; border-radius: var(--radius-sm); font-size: 0.78rem; cursor: pointer;
}
.btn-stop-timer:hover { background: rgba(239,68,68,0.3); }

/* 빈 상태 */
.tsp-empty { text-align: center; padding: 60px 24px; color: var(--text-muted); }
.tsp-empty-icon { margin-bottom: 14px; opacity: 0.3; }
.tsp-empty p { font-size: 0.9rem; margin-bottom: 6px; }
.tsp-empty-hint { font-size: 0.8rem; }

/* 카드 그리드 */
.tsp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.member-scrum-card {
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg);
  padding: 16px; transition: box-shadow var(--transition-base); box-shadow: var(--shadow-xs);
}
.member-scrum-card:hover { box-shadow: var(--shadow-sm); }
.card-speaker {
  border-color: var(--color-project); box-shadow: 0 0 0 3px rgba(48,127,226,0.15);
}
.card-blocker { border-left: 4px solid var(--color-danger); }

/* 카드 헤더 */
.msc-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.msc-avatar {
  width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-project), #8B5CF6);
  color: white; font-size: 0.875rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.msc-name-wrap { flex: 1; }
.msc-name { font-size: 0.9rem; font-weight: 700; color: var(--text-primary); }
.speaking-badge {
  font-size: 0.7rem; background: rgba(48,127,226,0.10); color: var(--color-project);
  padding: 1px 6px; border-radius: var(--radius-sm); font-weight: 600;
}
.msc-energy { display: flex; gap: 2px; font-size: 0.55rem; }
.energy-dot { color: var(--border-color); }
.energy-dot.filled { color: var(--color-warning); }

/* 집중 목표 */
.msc-focus {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.78rem; color: var(--text-secondary); margin-bottom: 10px;
  background: var(--bg-hover); padding: 5px 9px; border-radius: var(--radius-sm);
}

/* 태스크 진행률 */
.msc-tasks { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.msc-task-bar-wrap { flex: 1; height: 6px; background: var(--bg-hover); border-radius: 10px; overflow: hidden; }
.msc-task-bar-fill { height: 100%; border-radius: 10px; transition: width 0.3s; }
.msc-task-count { font-size: 0.72rem; color: var(--text-secondary); white-space: nowrap; }
.msc-no-tasks { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 8px; }

/* 블로커 */
.msc-blocker {
  display: flex; align-items: flex-start; gap: 5px;
  font-size: 0.775rem; padding: 6px 9px; border-radius: var(--radius-sm); margin-bottom: 8px;
}
.msc-blocker.sev-high   { background: rgba(220,38,38,0.08); color: var(--color-danger); }
.msc-blocker.sev-medium { background: rgba(217,119,6,0.08); color: #D97706; }
.msc-blocker.sev-low    { background: rgba(22,163,74,0.08); color: #16A34A; }
.blocker-icon { flex-shrink: 0; }
.blocker-text { line-height: 1.4; }

/* 할 일 목록 */
.msc-task-list { margin-top: 4px; margin-bottom: 4px; }
.msc-task-item {
  display: flex; gap: 6px; align-items: baseline;
  font-size: 0.78rem; color: var(--text-secondary); padding: 3px 0;
  border-bottom: 1px solid var(--border-color);
}
.msc-task-item:last-child { border-bottom: none; }
.task-dot { font-size: 0.65rem; flex-shrink: 0; color: var(--text-muted); }
.task-dot.done { color: var(--color-success); }
.task-done { text-decoration: line-through; color: var(--text-muted); }

.btn-expand {
  width: 100%; padding: 5px; background: transparent;
  border: none; border-top: 1px solid var(--border-color);
  font-size: 0.72rem; color: var(--text-muted); cursor: pointer; margin-top: 4px;
  text-align: center;
}
.btn-expand:hover { color: var(--color-project); }
</style>
