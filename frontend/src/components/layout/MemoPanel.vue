<template>
  <transition name="memo-slide">
    <div v-if="open" class="memo-panel" :style="{ left: sidebarCollapsed ? '68px' : '260px' }">
      <!-- 헤더 -->
      <div class="memo-header">
        <div class="memo-header-title">
          <StickyNote :size="15" />
          <span>메모장</span>
          <span class="memo-count">{{ notes.length }}</span>
        </div>
        <button class="memo-close-btn" @click="$emit('close')" title="닫기">
          <X :size="16" />
        </button>
      </div>

      <!-- 노트 목록 -->
      <div class="memo-list">
        <div
          v-for="note in notes"
          :key="note.id"
          class="memo-list-item"
          :class="{ active: activeNoteId === note.id }"
          @click="selectNote(note.id)"
        >
          <div class="note-item-content">
            <span class="note-title">{{ noteTitle(note) }}</span>
            <span class="note-date">{{ noteDate(note) }}</span>
          </div>
          <button class="note-delete-btn" @click.stop="deleteNote(note.id)" title="삭제">
            <Trash2 :size="12" />
          </button>
        </div>

        <button class="memo-new-btn" @click="createNote">
          <Plus :size="14" />
          새 메모
        </button>
      </div>

      <!-- 에디터 -->
      <div class="memo-editor" v-if="activeNote">
        <textarea
          ref="textareaRef"
          v-model="activeNote.content"
          class="memo-textarea"
          placeholder="여기에 자유롭게 메모를 입력하세요..."
          @input="onInput"
          spellcheck="false"
        />
      </div>
      <div v-else class="memo-empty">
        <StickyNote :size="32" style="opacity:0.25" />
        <p>메모를 선택하거나 새로 만드세요</p>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { StickyNote, X, Plus, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  sidebarCollapsed: Boolean
})
defineEmits(['close'])

const STORAGE_KEY = 'workoop-memos'

function loadNotes() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
  } catch {
    return []
  }
}

const notes = ref(loadNotes())
const activeNoteId = ref(notes.value[0]?.id ?? null)
const textareaRef = ref()

const activeNote = computed(() =>
  notes.value.find(n => n.id === activeNoteId.value) ?? null
)

function noteTitle(note) {
  const first = note.content.split('\n')[0].trim()
  return first || '(빈 메모)'
}

function noteDate(note) {
  const d = new Date(note.updatedAt)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${mm}/${dd} ${hh}:${min}`
}

function saveNotes() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(notes.value))
}

let saveTimer = null
function onInput() {
  if (activeNote.value) activeNote.value.updatedAt = Date.now()
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveNotes, 400)
}

function createNote() {
  const note = { id: Date.now().toString(), content: '', updatedAt: Date.now() }
  notes.value.unshift(note)
  activeNoteId.value = note.id
  saveNotes()
  nextTick(() => textareaRef.value?.focus())
}

function selectNote(id) {
  activeNoteId.value = id
  nextTick(() => textareaRef.value?.focus())
}

function deleteNote(id) {
  const idx = notes.value.findIndex(n => n.id === id)
  if (idx === -1) return
  notes.value.splice(idx, 1)
  if (activeNoteId.value === id) {
    activeNoteId.value = notes.value[0]?.id ?? null
  }
  saveNotes()
}

// 패널이 열릴 때 노트가 없으면 자동 생성
watch(() => props.open, (val) => {
  if (val && notes.value.length === 0) createNote()
  else if (val) nextTick(() => textareaRef.value?.focus())
})
</script>

<style scoped>
.memo-panel {
  position: fixed;
  top: 0;
  width: 300px;
  height: 100vh;
  background: #1D1E20;
  border-right: 1px solid rgba(255,255,255,0.07);
  box-shadow: 4px 0 24px rgba(0,0,0,0.35);
  z-index: 150;
  display: flex;
  flex-direction: column;
  transition: left 0.25s ease;
}

/* 슬라이드 애니메이션 */
.memo-slide-enter-active,
.memo-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.memo-slide-enter-from,
.memo-slide-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}

/* 헤더 */
.memo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px 12px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  flex-shrink: 0;
}
.memo-header-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #94A3B8;
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.memo-count {
  background: rgba(59,130,246,0.25);
  color: #60A5FA;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 99px;
}
.memo-close-btn {
  width: 26px; height: 26px;
  border: none;
  background: rgba(255,255,255,0.05);
  border-radius: 6px;
  color: #64748B;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.memo-close-btn:hover { background: rgba(255,255,255,0.1); color: #94A3B8; }

/* 노트 목록 */
.memo-list {
  padding: 8px 8px 4px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  max-height: 220px;
  overflow-y: auto;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.memo-list::-webkit-scrollbar { width: 4px; }
.memo-list::-webkit-scrollbar-track { background: transparent; }
.memo-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }

.memo-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s;
  gap: 6px;
}
.memo-list-item:hover { background: rgba(255,255,255,0.05); }
.memo-list-item.active { background: rgba(59,130,246,0.15); }
.note-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.note-title {
  font-size: 0.82rem;
  font-weight: 500;
  color: #CBD5E1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 190px;
}
.memo-list-item.active .note-title { color: #93C5FD; }
.note-date {
  font-size: 0.68rem;
  color: #475569;
}
.note-delete-btn {
  width: 22px; height: 22px;
  border: none; background: transparent;
  color: #475569; cursor: pointer;
  border-radius: 5px;
  display: flex; align-items: center; justify-content: center;
  opacity: 0;
  transition: all 0.12s;
}
.memo-list-item:hover .note-delete-btn { opacity: 1; }
.note-delete-btn:hover { background: rgba(239,68,68,0.2); color: #F87171; }

.memo-new-btn {
  display: flex; align-items: center; gap: 6px;
  width: 100%; padding: 7px 10px;
  border: 1px dashed rgba(255,255,255,0.1);
  border-radius: 8px;
  background: transparent; color: #475569;
  cursor: pointer; font-size: 0.8rem;
  transition: all 0.15s;
  margin-top: 2px;
}
.memo-new-btn:hover { border-color: rgba(59,130,246,0.4); color: #60A5FA; background: rgba(59,130,246,0.08); }

/* 에디터 */
.memo-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px 10px;
}
.memo-textarea {
  flex: 1;
  width: 100%;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  color: #CBD5E1;
  font-size: 0.875rem;
  line-height: 1.7;
  font-family: 'Pretendard', 'Apple SD Gothic Neo', sans-serif;
  caret-color: #60A5FA;
}
.memo-textarea::placeholder { color: #334155; }
.memo-textarea::-webkit-scrollbar { width: 4px; }
.memo-textarea::-webkit-scrollbar-track { background: transparent; }
.memo-textarea::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 4px; }

.memo-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #334155;
  font-size: 0.82rem;
}
</style>
