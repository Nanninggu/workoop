<template>
  <div class="kanban-page">

    <!-- 상단 헤더 -->
    <div class="kanban-header">
      <div class="header-left">
        <button class="btn-back" @click="$router.push('/projects')">
          <ChevronLeft :size="18" /> 프로젝트
        </button>
        <h1 class="project-name">{{ project?.name }}</h1>
      </div>

      <!-- 팀 KPI 미니바 -->
      <div class="kpi-minibar" v-if="linkedKpis.length > 0">
        <div v-for="kpi in linkedKpis" :key="kpi.kpiId" class="kpi-mini-item">
          <span class="kpi-mini-name">{{ kpi.name }}</span>
          <div class="kpi-mini-bar-wrap">
            <div class="kpi-mini-bar-fill" :style="{ width: kpi.rate + '%', background: rateColor(kpi.rate) }"></div>
          </div>
          <span class="kpi-mini-rate" :style="{ color: rateColor(kpi.rate) }">{{ kpi.rate }}%</span>
        </div>
      </div>

      <button class="btn-add-task" @click="openTaskForm(null, 'BACKLOG')">
        <Plus :size="16" /> 태스크 추가
      </button>
    </div>

    <!-- 칸반 보드 -->
    <div class="kanban-board" v-if="!loading">
      <div v-for="col in columns" :key="col.status" class="kanban-col">
        <div class="col-header">
          <span class="col-dot" :style="{ background: col.color }"></span>
          <span class="col-title">{{ col.label }}</span>
          <span class="col-count">{{ tasksByStatus[col.status]?.length || 0 }}</span>
        </div>

        <draggable
          :list="tasksByStatus[col.status]"
          group="tasks"
          item-key="id"
          class="col-body"
          ghost-class="task-ghost"
          @change="(evt) => onDragChange(evt, col.status)"
        >
          <template #item="{ element: task }">
            <div class="task-card" @click="openTaskForm(task, col.status)">
              <!-- 우선순위 바 -->
              <div class="priority-bar" :style="{ background: priorityColor(task.priority) }"></div>

              <div class="task-content">
                <div class="task-title">{{ task.title }}</div>

                <!-- KPI 연동 표시 -->
                <div v-if="task.kpiId" class="task-kpi-badge">
                  <Target :size="11" />
                  {{ task.kpiName }}
                  <span v-if="task.kpiContribution" class="kpi-contrib">
                    +{{ task.kpiContribution }}{{ task.kpiUnit || '' }}
                  </span>
                </div>

                <div class="task-footer">
                  <span v-if="task.dueDate" class="task-due" :class="{ overdue: isDueOver(task.dueDate) }">
                    <Calendar :size="11" /> {{ formatDate(task.dueDate) }}
                  </span>
                  <div class="task-assignees">
                    <div
                      v-for="(uid, i) in parseAssigneeIds(task.assigneeIds, task.assigneeId)"
                      :key="uid"
                      class="task-assignee"
                      :style="{ marginLeft: i > 0 ? '-6px' : '0', zIndex: 10 - i }"
                      :title="memberName(uid)"
                    >
                      {{ memberInitial(uid) }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template #footer>
            <button class="btn-add-inline" @click="openTaskForm(null, col.status)">
              <Plus :size="13" /> 추가
            </button>
          </template>
        </draggable>
      </div>
    </div>

    <div v-else class="kanban-loading">불러오는 중...</div>

    <!-- 태스크 슬라이드 패널 -->
    <transition name="slide">
      <div v-if="panelOpen" class="task-panel-overlay" @click.self="closePanel">
        <div class="task-panel">
          <div class="panel-header">
            <h2>{{ editingTask ? '태스크 수정' : '새 태스크' }}</h2>
            <button @click="closePanel" class="btn-close"><X :size="20" /></button>
          </div>

          <div class="panel-body">
            <div class="field-group">
              <label>제목 *</label>
              <input v-model="taskForm.title" class="field-input" placeholder="태스크 이름을 입력하세요" />
            </div>

            <div class="field-row">
              <div class="field-group">
                <label>상태</label>
                <select v-model="taskForm.status" class="field-select">
                  <option v-for="c in columns" :key="c.status" :value="c.status">{{ c.label }}</option>
                </select>
              </div>
              <div class="field-group">
                <label>우선순위</label>
                <select v-model="taskForm.priority" class="field-select">
                  <option value="P1">🔴 P1 긴급</option>
                  <option value="P2">🟡 P2 보통</option>
                  <option value="P3">🟢 P3 낮음</option>
                </select>
              </div>
            </div>

            <div class="field-group">
              <label>담당자 <span class="label-hint">(복수 선택 가능)</span></label>
              <div class="assignee-multi">
                <div
                  v-for="m in members"
                  :key="m.userId"
                  class="assignee-chip"
                  :class="{ selected: taskForm.assigneeIds.includes(m.userId) }"
                  @click="toggleAssignee(m.userId)"
                >
                  <span class="chip-avatar">{{ m.userName.charAt(0) }}</span>
                  {{ m.userName }}
                </div>
              </div>
            </div>

            <div class="field-group kpi-link-group">
              <label>
                <Target :size="14" style="display:inline;vertical-align:middle;margin-right:4px;" />
                연결 KPI <span class="label-hint">(완료 시 자동 반영)</span>
              </label>
              <select v-model="taskForm.kpiId" class="field-select">
                <option :value="null">KPI 연결 안 함</option>
                <option v-for="k in myKpis" :key="k.id" :value="k.id">
                  {{ k.name }} ({{ k.kpiType }})
                </option>
              </select>
              <div v-if="taskForm.kpiId" class="contribution-row">
                <input
                  v-model.number="taskForm.kpiContribution"
                  type="number" step="0.1" min="0"
                  class="field-input contrib-input"
                  placeholder="기여도 수치"
                />
                <span class="contrib-unit">{{ selectedKpiUnit }}</span>
                <span class="contrib-hint">완료 시 KPI에 이 값만큼 자동 반영</span>
              </div>
            </div>

            <div class="field-row">
              <div class="field-group">
                <label>마감일</label>
                <input v-model="taskForm.dueDate" type="date" class="field-input" />
              </div>
              <div class="field-group">
                <label>예상 시간(h)</label>
                <input v-model.number="taskForm.estimatedHours" type="number" step="0.5" min="0" class="field-input" placeholder="0.0" />
              </div>
            </div>

            <div class="field-group">
              <label>설명</label>
              <textarea v-model="taskForm.description" class="field-input" rows="4" placeholder="상세 내용을 입력하세요"></textarea>
            </div>
          </div>

          <div class="panel-footer">
            <button v-if="editingTask" class="btn-delete" @click="deleteTask">삭제</button>
            <div style="flex:1"></div>
            <button class="btn-cancel" @click="closePanel">취소</button>
            <button class="btn-save" :disabled="!taskForm.title?.trim() || saving" @click="saveTask">
              {{ saving ? '저장 중...' : (editingTask ? '수정' : '추가') }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrgStore } from '@/store/orgStore'
import { useKpiStore } from '@/store/kpiStore'
import { useWsStore } from '@/store/wsStore'
import { projectApi, taskApi } from '@/api/projectApi'
import { orgApi } from '@/api/orgApi'
import draggable from 'vuedraggable'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChevronLeft, Target, Calendar, X } from 'lucide-vue-next'
import dayjs from 'dayjs'

const route    = useRoute()
const router   = useRouter()
const orgStore = useOrgStore()
const kpiStore = useKpiStore()
const wsStore  = useWsStore()

const project  = ref(null)
const tasks    = ref([])
const members  = ref([])
const loading  = ref(true)
const panelOpen    = ref(false)
const editingTask  = ref(null)
const saving       = ref(false)

const columns = [
  { status: 'BACKLOG',     label: '백로그',    color: '#94A3B8' },
  { status: 'TODO',        label: '할 일',     color: '#3B82F6' },
  { status: 'IN_PROGRESS', label: '진행 중',   color: '#F59E0B' },
  { status: 'REVIEW',      label: '검토',      color: '#8B5CF6' },
  { status: 'DONE',        label: '완료',      color: '#10B981' },
]

const emptyForm = () => ({
  title: '', description: '', status: 'BACKLOG', priority: 'P2',
  assigneeIds: [], assigneeId: null, kpiId: null, kpiContribution: null,
  dueDate: null, estimatedHours: null,
})

function parseAssigneeIds(idsJson, fallbackId) {
  if (idsJson) {
    try { return JSON.parse(idsJson) } catch { /* fall through */ }
  }
  return fallbackId ? [fallbackId] : []
}
function memberName(uid) {
  return members.value.find(m => m.userId === uid)?.userName || String(uid)
}
function memberInitial(uid) {
  return memberName(uid).charAt(0)
}
function toggleAssignee(uid) {
  const ids = taskForm.value.assigneeIds
  const idx = ids.indexOf(uid)
  if (idx === -1) ids.push(uid)
  else ids.splice(idx, 1)
  taskForm.value.assigneeId = ids[0] ?? null
}
const taskForm = ref(emptyForm())

const tasksByStatus = computed(() => {
  const map = {}
  columns.forEach(c => { map[c.status] = [] })
  tasks.value.forEach(t => {
    if (map[t.status]) map[t.status].push(t)
    else map['BACKLOG'].push(t)
  })
  return map
})

const myKpis = computed(() => kpiStore.kpis || [])

const selectedKpiUnit = computed(() => {
  const k = myKpis.value.find(k => k.id === taskForm.value.kpiId)
  return k?.unit || ''
})

// 보드에 연결된 KPI 달성률 집계 (미니바용)
const linkedKpis = computed(() => {
  const kpiIds = [...new Set(tasks.value.filter(t => t.kpiId).map(t => t.kpiId))]
  return kpiIds.map(id => {
    const t = tasks.value.find(t => t.kpiId === id)
    const kpi = myKpis.value.find(k => k.id === id)
    const done = tasks.value.filter(t => t.kpiId === id && t.status === 'DONE').length
    const total = tasks.value.filter(t => t.kpiId === id).length
    const rate = total > 0 ? Math.round((done / total) * 100) : 0
    return { kpiId: id, name: t?.kpiName || kpi?.name || '', rate }
  }).filter(k => k.name)
})

function rateColor(rate) {
  if (rate >= 80) return '#10B981'
  if (rate >= 50) return '#F59E0B'
  return '#EF4444'
}

function priorityColor(p) {
  return p === 'P1' ? '#EF4444' : p === 'P2' ? '#F59E0B' : '#10B981'
}

function formatDate(d) { return d ? dayjs(d).format('MM/DD') : '' }
function isDueOver(d) { return d && dayjs(d).isBefore(dayjs(), 'day') }

async function load() {
  loading.value = true
  try {
    const [projRes, taskRes] = await Promise.all([
      projectApi.get(route.params.id),
      taskApi.list(route.params.id),
    ])
    project.value = projRes.data
    tasks.value   = taskRes.data || []

    if (orgStore.currentOrg) {
      const mRes = await orgApi.members(orgStore.currentOrg.id)
      members.value = mRes.data || []
    }
    await kpiStore.fetchKpis()
  } finally {
    loading.value = false
  }
}

function openTaskForm(task, defaultStatus) {
  if (task) {
    editingTask.value = task
    const ids = parseAssigneeIds(task.assigneeIds, task.assigneeId)
    taskForm.value = {
      title: task.title, description: task.description,
      status: task.status, priority: task.priority,
      assigneeIds: ids, assigneeId: ids[0] ?? null,
      kpiId: task.kpiId, kpiContribution: task.kpiContribution,
      dueDate: task.dueDate, estimatedHours: task.estimatedHours,
    }
  } else {
    editingTask.value = null
    taskForm.value = { ...emptyForm(), status: defaultStatus || 'BACKLOG' }
  }
  panelOpen.value = true
}

function closePanel() { panelOpen.value = false }

async function saveTask() {
  if (!taskForm.value.title?.trim()) return
  saving.value = true
  try {
    const ids = taskForm.value.assigneeIds
    const payload = {
      ...taskForm.value,
      projectId: Number(route.params.id),
      assigneeId: ids[0] ?? null,
      assigneeIds: ids.length > 0 ? JSON.stringify(ids) : null,
      kpiContribution: taskForm.value.kpiContribution || null,
      estimatedHours: taskForm.value.estimatedHours || null,
    }
    if (editingTask.value) {
      const prevStatus = editingTask.value.status
      await taskApi.update(editingTask.value.id, payload)
      // 상태가 변경됐으면 KPI 자동 반영 트리거
      if (prevStatus !== payload.status) {
        await taskApi.changeStatus(editingTask.value.id, payload.status)
      }
      ElMessage.success('수정되었습니다.')
    } else {
      await taskApi.create(payload)
      ElMessage.success('태스크가 추가되었습니다.')
    }
    closePanel()
    await load()
  } finally {
    saving.value = false
  }
}

async function deleteTask() {
  await ElMessageBox.confirm('이 태스크를 삭제할까요?', '삭제', {
    confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning'
  })
  await taskApi.delete(editingTask.value.id)
  ElMessage.success('삭제되었습니다.')
  closePanel()
  await load()
}

// 드래그앤드롭 완료 시 상태 변경 (KPI 자동 반영 포함)
async function onDragChange(evt, targetStatus) {
  if (!evt.added) return  // 이 컬럼에 카드가 추가됐을 때만 처리
  const task = evt.added.element
  if (!task || task.status === targetStatus) return
  try {
    await taskApi.changeStatus(task.id, targetStatus)
    task.status = targetStatus
    if (targetStatus === 'DONE' && task.kpiId && task.kpiContribution) {
      ElMessage.success(`✅ "${task.kpiName}" KPI에 ${task.kpiContribution}${task.kpiUnit || ''} 반영됨!`)
      await kpiStore.fetchDashboard()
    }
  } catch {
    await load()
  }
}

// ── WebSocket 실시간 태스크 동기화 ───────────────────────────────────────────
const _wsUnsubs = []
const currentProjectId = computed(() => Number(route.params.id))

function _applyTaskEvent(event) {
  const t = event.payload
  if (!t || t.projectId !== currentProjectId.value) return

  if (event.type === 'TASK_DELETED') {
    tasks.value = tasks.value.filter(x => x.id !== t.id)
    return
  }
  const idx = tasks.value.findIndex(x => x.id === t.id)
  if (idx > -1) {
    tasks.value[idx] = t          // TASK_UPDATED / TASK_STATUS_CHANGED
  } else {
    tasks.value.unshift(t)        // TASK_CREATED
  }
}

onMounted(async () => {
  await load()
  // WS가 이미 연결된 경우 즉시 구독, 아직 연결 중이면 on()이 나중에 발동됨
  _wsUnsubs.push(wsStore.on('TASK_CREATED',        _applyTaskEvent))
  _wsUnsubs.push(wsStore.on('TASK_UPDATED',        _applyTaskEvent))
  _wsUnsubs.push(wsStore.on('TASK_STATUS_CHANGED', _applyTaskEvent))
  _wsUnsubs.push(wsStore.on('TASK_DELETED',        _applyTaskEvent))
})

onUnmounted(() => {
  _wsUnsubs.forEach(fn => fn())
})
</script>

<style scoped>
.kanban-page { display: flex; flex-direction: column; height: 100vh; overflow: hidden; background: var(--bg-main); }

/* 헤더 */
.kanban-header {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 24px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-color); flex-shrink: 0; flex-wrap: wrap;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.btn-back {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 10px; background: var(--bg-hover); border: none;
  border-radius: var(--radius-md); font-size: 0.825rem; color: var(--text-secondary);
  cursor: pointer; font-weight: 500;
}
.btn-back:hover { background: var(--border-color); }
.project-name { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); }

/* KPI 미니바 */
.kpi-minibar { display: flex; gap: 16px; flex: 1; flex-wrap: wrap; }
.kpi-mini-item { display: flex; align-items: center; gap: 6px; }
.kpi-mini-name { font-size: 0.75rem; font-weight: 600; color: var(--text-secondary); white-space: nowrap; }
.kpi-mini-bar-wrap { width: 80px; height: 6px; background: var(--bg-hover); border-radius: 10px; overflow: hidden; }
.kpi-mini-bar-fill { height: 100%; border-radius: 10px; transition: width 0.4s ease; }
.kpi-mini-rate { font-size: 0.75rem; font-weight: 700; font-family: 'JetBrains Mono', monospace; }

.btn-add-task {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.85rem;
  font-weight: 600; cursor: pointer; margin-left: auto;
}
.btn-add-task:hover { opacity: 0.88; }

/* 보드 */
.kanban-board {
  display: flex; gap: 12px; padding: 20px 24px;
  overflow-x: auto; flex: 1; align-items: flex-start;
}
.kanban-loading { display: flex; align-items: center; justify-content: center; flex: 1; color: var(--text-muted); }

.kanban-col {
  width: 272px; min-width: 272px; display: flex; flex-direction: column;
  background: var(--bg-hover); border-radius: var(--radius-lg); overflow: hidden;
}
.col-header {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px; font-size: 0.85rem; font-weight: 700; color: var(--text-primary);
}
.col-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.col-title { flex: 1; }
.col-count {
  background: var(--bg-card); color: var(--text-secondary); font-size: 0.72rem; font-weight: 700;
  padding: 1px 7px; border-radius: 10px;
}

.col-body { padding: 6px 8px 8px; min-height: 100px; display: flex; flex-direction: column; gap: 8px; }

/* 태스크 카드 */
.task-card {
  background: var(--bg-card); border-radius: var(--radius-md); cursor: pointer;
  border: 1px solid var(--border-color); display: flex; overflow: hidden;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}
.task-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.task-ghost { opacity: 0.4; background: rgba(48,127,226,0.10); border: 2px dashed var(--color-project); }

.priority-bar { width: 4px; flex-shrink: 0; }
.task-content { padding: 10px 12px; flex: 1; min-width: 0; }
.task-title { font-size: 0.875rem; font-weight: 600; color: var(--text-primary); line-height: 1.4; margin-bottom: 6px; }

.task-kpi-badge {
  display: inline-flex; align-items: center; gap: 4px;
  background: rgba(48,127,226,0.08); color: var(--color-project); font-size: 0.72rem; font-weight: 600;
  padding: 2px 8px; border-radius: 20px; margin-bottom: 6px;
}
.kpi-contrib { color: var(--color-success); font-weight: 700; }

.task-footer { display: flex; align-items: center; justify-content: space-between; }
.task-due { display: flex; align-items: center; gap: 3px; font-size: 0.72rem; color: var(--text-muted); }
.task-due.overdue { color: var(--color-danger); }
.task-assignees { display: flex; align-items: center; }
.task-assignee {
  width: 22px; height: 22px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-project), #8B5CF6);
  color: white; font-size: 0.65rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  border: 1.5px solid var(--bg-card); position: relative;
}

/* 멀티 담당자 선택 칩 */
.assignee-multi {
  display: flex; flex-wrap: wrap; gap: 6px;
}
.assignee-chip {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 10px; border-radius: 99px;
  border: 1.5px solid var(--border-color);
  background: var(--bg-hover); cursor: pointer;
  font-size: 0.8rem; color: var(--text-secondary);
  transition: all var(--transition-fast);
  user-select: none;
}
.assignee-chip:hover { border-color: var(--color-project); color: var(--color-project); }
.assignee-chip.selected {
  background: rgba(48,127,226,0.1);
  border-color: var(--color-project);
  color: var(--color-project);
  font-weight: 600;
}
.chip-avatar {
  width: 20px; height: 20px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-project), #8B5CF6);
  color: white; font-size: 0.65rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.btn-add-inline {
  display: flex; align-items: center; gap: 4px; width: 100%;
  padding: 7px 10px; background: transparent; border: 1px dashed var(--border-color);
  border-radius: var(--radius-md); color: var(--text-muted); font-size: 0.8rem; cursor: pointer;
  transition: all var(--transition-fast); justify-content: center;
}
.btn-add-inline:hover { background: var(--bg-card); color: var(--color-project); border-color: var(--color-project); }

/* 슬라이드 패널 */
.task-panel-overlay {
  position: fixed; inset: 0; background: rgba(17,24,39,0.45);
  z-index: 1000; display: flex; justify-content: flex-end;
}
.task-panel {
  width: 480px; max-width: 90vw; background: var(--bg-card);
  height: 100%; display: flex; flex-direction: column;
  box-shadow: var(--shadow-xl);
}
.slide-enter-active, .slide-leave-active { transition: transform 0.25s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; border-bottom: 1px solid var(--border-color);
}
.panel-header h2 { font-size: 1.05rem; font-weight: 700; color: var(--text-primary); }
.btn-close { background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; }
.btn-close:hover { color: var(--text-primary); }

.panel-body { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }

.field-group { display: flex; flex-direction: column; gap: 5px; }
.field-group label { font-size: 0.78rem; font-weight: 600; color: var(--text-secondary); }
.label-hint { font-weight: 400; color: var(--text-muted); }
.field-input {
  padding: 9px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  font-size: 0.875rem; outline: none; transition: border-color var(--transition-fast);
  width: 100%; box-sizing: border-box; background: var(--bg-card); color: var(--text-primary);
}
.field-input:focus { border-color: var(--color-project); }
.field-select {
  padding: 9px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  font-size: 0.875rem; background: var(--bg-card); color: var(--text-primary); outline: none; cursor: pointer;
  width: 100%;
}
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.kpi-link-group { background: rgba(16,185,129,0.06); padding: 12px; border-radius: var(--radius-md); border: 1px solid rgba(16,185,129,0.22); }
.contribution-row { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.contrib-input { width: 100px; flex-shrink: 0; }
.contrib-unit { font-size: 0.8rem; color: var(--text-secondary); font-weight: 600; }
.contrib-hint { font-size: 0.72rem; color: var(--color-success); font-weight: 500; }

.panel-footer {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 24px; border-top: 1px solid var(--border-color);
}
.btn-delete {
  padding: 8px 14px; background: transparent; border: 1px solid rgba(239,68,68,0.4);
  color: var(--color-danger); border-radius: var(--radius-md); font-size: 0.825rem; cursor: pointer;
}
.btn-delete:hover { background: rgba(239,68,68,0.06); }
.btn-cancel {
  padding: 9px 16px; background: var(--bg-hover); border: none;
  border-radius: var(--radius-md); font-size: 0.875rem; color: var(--text-secondary); cursor: pointer;
}
.btn-save {
  padding: 9px 20px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.875rem;
  font-weight: 600; cursor: pointer;
}
.btn-save:hover:not(:disabled) { opacity: 0.88; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
