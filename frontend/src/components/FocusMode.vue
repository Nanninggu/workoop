<template>
  <teleport to="body">
    <transition name="focus-fade">
      <div v-if="focus.active" class="focus-overlay">

        <!-- 상단 미니바 -->
        <div class="focus-topbar">
          <!-- 현재 태스크 -->
          <div class="focus-task-area">
            <div class="focus-task-label">집중 중</div>
            <div class="focus-task-title" v-if="focus.currentTask">
              {{ focus.currentTask.title }}
              <span v-if="focus.currentTask.kpiName" class="focus-kpi-tag">
                <Target :size="11" /> {{ focus.currentTask.kpiName }}
              </span>
            </div>
            <div v-else class="focus-task-title muted">태스크를 선택하세요</div>
          </div>

          <!-- KPI 게이지 (현재 태스크에 KPI 연결 시) -->
          <div class="focus-kpi-area" v-if="linkedKpi">
            <div class="focus-kpi-name">{{ linkedKpi.name }}</div>
            <div class="focus-kpi-bar-wrap">
              <div class="focus-kpi-bar-fill"
                :style="{ width: kpiRate + '%', background: rateColor(kpiRate) }"
              ></div>
            </div>
            <div class="focus-kpi-pct" :style="{ color: rateColor(kpiRate) }">{{ kpiRate }}%</div>
          </div>

          <!-- 포모도로 타이머 -->
          <div class="focus-timer-area">
            <div class="focus-phase-label" :class="focus.phase">{{ focus.phaseLabel }}</div>
            <div class="focus-time">{{ focus.timeLabel }}</div>
            <div class="focus-timer-controls">
              <button v-if="!focus.running" @click="focus.startTimer()" class="focus-btn-start">
                <Play :size="14" /> 시작
              </button>
              <button v-else @click="focus.pauseTimer()" class="focus-btn-pause">
                <Pause :size="14" /> 일시정지
              </button>
              <button @click="focus.resetTimer()" class="focus-btn-reset" title="리셋">
                <RotateCcw :size="13" />
              </button>
            </div>
            <div class="focus-pomo-count">
              오늘 완료한 포모도로
              <span class="pomo-dots">
                <span v-for="i in Math.min(focus.todayPomos, 8)" :key="i" class="pomo-dot filled"></span>
                <span v-for="i in Math.max(0, 4 - focus.todayPomos % 4)" :key="'e'+i" class="pomo-dot"></span>
              </span>
              {{ focus.todayPomos }}세트
            </div>
          </div>

          <!-- 종료 버튼 -->
          <button @click="exitFocus" class="focus-btn-exit">
            <X :size="16" /> 집중 모드 종료
          </button>
        </div>

        <!-- 메인 콘텐츠 영역 -->
        <div class="focus-body">
          <!-- 태스크 선택 패널 -->
          <div class="focus-content">
            <div class="focus-current-task" v-if="focus.currentTask">
              <div class="fct-priority-dot" :style="{ background: priorityColor(focus.currentTask.priority) }"></div>
              <div class="fct-info">
                <div class="fct-title">{{ focus.currentTask.title }}</div>
                <div class="fct-desc" v-if="focus.currentTask.description">
                  {{ focus.currentTask.description }}
                </div>
                <div class="fct-meta">
                  <span v-if="focus.currentTask.dueDate" class="fct-due">
                    <Calendar :size="12" /> {{ formatDate(focus.currentTask.dueDate) }}
                  </span>
                  <span v-if="focus.currentTask.estimatedHours" class="fct-hours">
                    <Clock :size="12" /> {{ focus.currentTask.estimatedHours }}h 예상
                  </span>
                </div>
              </div>
              <button class="btn-complete-task" @click="completeCurrentTask">
                <Check :size="16" /> 완료
              </button>
            </div>

            <!-- 진행률 링 -->
            <div class="focus-ring-wrap">
              <svg class="focus-ring" viewBox="0 0 120 120">
                <circle cx="60" cy="60" r="52" class="ring-bg" />
                <circle
                  cx="60" cy="60" r="52"
                  class="ring-fill"
                  :class="focus.phase"
                  :stroke-dasharray="`${focus.progress * 3.267} 326.7`"
                  stroke-linecap="round"
                />
              </svg>
              <div class="ring-inner">
                <div class="ring-time">{{ focus.timeLabel }}</div>
                <div class="ring-phase" :class="focus.phase">{{ focus.phaseLabel }}</div>
              </div>
            </div>

            <!-- 태스크 목록 -->
            <div class="focus-task-list" v-if="focus.myTasks.length > 0">
              <div class="task-list-label">내 태스크 목록</div>
              <div
                v-for="t in focus.myTasks"
                :key="t.id"
                class="focus-task-item"
                :class="{ selected: focus.currentTask?.id === t.id }"
                @click="focus.selectTask(t)"
              >
                <div class="fti-dot" :style="{ background: priorityColor(t.priority) }"></div>
                <div class="fti-info">
                  <div class="fti-title">{{ t.title }}</div>
                  <div class="fti-kpi" v-if="t.kpiName">
                    <Target :size="10" /> {{ t.kpiName }}
                  </div>
                </div>
                <Check v-if="focus.currentTask?.id === t.id" :size="16" class="fti-check" />
              </div>
            </div>
            <div v-else class="focus-no-tasks">
              담당 태스크가 없습니다.<br>
              칸반 보드에서 본인에게 태스크를 할당해 주세요.
            </div>
          </div>
        </div>

        <!-- 완료 confetti 애니메이션 -->
        <div v-if="showConfetti" class="confetti-wrap">
          <div v-for="i in 30" :key="i" class="confetti-piece"
            :style="{
              left: Math.random() * 100 + '%',
              animationDelay: Math.random() * 0.5 + 's',
              background: confettiColors[i % confettiColors.length]
            }"
          ></div>
        </div>

      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useFocusStore } from '@/store/focusStore'
import { useKpiStore } from '@/store/kpiStore'
import { taskApi } from '@/api/projectApi'
import { ElMessage } from 'element-plus'
import { Play, Pause, RotateCcw, X, Target, Calendar, Clock, Check } from 'lucide-vue-next'
import dayjs from 'dayjs'

const focus   = useFocusStore()
const kpiStore = useKpiStore()

const showConfetti = ref(false)
const confettiColors = ['#3B82F6','#10B981','#F59E0B','#EF4444','#8B5CF6','#F97316']

// 현재 태스크에 연결된 KPI
const linkedKpi = computed(() => {
  if (!focus.currentTask?.kpiId) return null
  return kpiStore.kpis.find(k => k.id === focus.currentTask.kpiId) || null
})

const kpiRate = computed(() => {
  if (!linkedKpi.value || !linkedKpi.value.targetValue) return 0
  const dashboard = kpiStore.dashboard
  if (!dashboard) return 0
  const summary = dashboard.kpiSummaries?.find(s => s.kpiId === linkedKpi.value.id)
  return summary?.achievementRate || 0
})

function rateColor(rate) {
  if (rate >= 80) return '#10B981'
  if (rate >= 50) return '#F59E0B'
  return '#EF4444'
}

function priorityColor(p) {
  return p === 'P1' ? '#EF4444' : p === 'P2' ? '#F59E0B' : '#10B981'
}

function formatDate(d) {
  return d ? dayjs(d).format('MM/DD') : ''
}

async function completeCurrentTask() {
  if (!focus.currentTask) return
  await taskApi.changeStatus(focus.currentTask.id, 'DONE')

  // confetti 트리거
  showConfetti.value = true
  setTimeout(() => { showConfetti.value = false }, 2000)

  const taskTitle = focus.currentTask.title
  if (focus.currentTask.kpiId && focus.currentTask.kpiContribution) {
    ElMessage.success(`✅ "${taskTitle}" 완료! KPI에 ${focus.currentTask.kpiContribution}${focus.currentTask.kpiUnit || ''} 반영됨`)
    await kpiStore.fetchDashboard()
  } else {
    ElMessage.success(`✅ "${taskTitle}" 완료!`)
  }

  // 다음 태스크로 이동
  const res = await taskApi.mine()
  focus.myTasks = res.data || []
  focus.currentTask = focus.myTasks[0] || null
}

function exitFocus() {
  const alerts = focus.flushAlerts()
  focus.exit()
  alerts.forEach(msg => ElMessage.info(msg))
}

// 집중 모드 진입 시 KPI 데이터 로드
watch(() => focus.active, (val) => {
  if (val) kpiStore.fetchDashboard()
})

// 타이틀 업데이트 (집중 모드 중 브라우저 탭 표시)
watch(() => focus.timeLabel, (t) => {
  if (focus.active) document.title = `${t} ${focus.phaseLabel} — CoopWork`
  else document.title = document.title.replace(/^\d+:\d+ .*— /, '')
})
</script>

<style scoped>
.focus-overlay {
  position: fixed; inset: 0; z-index: 9000;
  background: rgba(10, 14, 26, 0.96);
  display: flex; flex-direction: column;
  backdrop-filter: blur(8px);
}
.focus-fade-enter-active, .focus-fade-leave-active { transition: opacity 0.3s ease; }
.focus-fade-enter-from, .focus-fade-leave-to { opacity: 0; }

/* 상단 미니바 */
.focus-topbar {
  display: flex; align-items: center; gap: 20px;
  padding: 14px 28px; border-bottom: 1px solid rgba(255,255,255,0.08);
  background: rgba(15, 23, 42, 0.85); flex-shrink: 0; flex-wrap: wrap;
}

.focus-task-area { flex: 1; min-width: 160px; }
.focus-task-label { font-size: 0.65rem; font-weight: 700; color: rgba(255,255,255,0.35); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
.focus-task-title { font-size: 0.9rem; font-weight: 700; color: white; display: flex; align-items: center; gap: 8px; }
.focus-task-title.muted { color: rgba(255,255,255,0.3); font-weight: 400; }
.focus-kpi-tag {
  display: inline-flex; align-items: center; gap: 3px;
  background: rgba(48,127,226,0.2); color: #93C5FD;
  font-size: 0.7rem; font-weight: 600; padding: 2px 8px; border-radius: 10px;
}

.focus-kpi-area { display: flex; align-items: center; gap: 8px; }
.focus-kpi-name { font-size: 0.75rem; color: rgba(255,255,255,0.5); font-weight: 600; white-space: nowrap; }
.focus-kpi-bar-wrap { width: 100px; height: 6px; background: rgba(255,255,255,0.1); border-radius: 10px; overflow: hidden; }
.focus-kpi-bar-fill { height: 100%; border-radius: 10px; transition: width 0.4s; }
.focus-kpi-pct { font-size: 0.8rem; font-weight: 800; white-space: nowrap; font-family: 'JetBrains Mono', monospace; }

.focus-timer-area { display: flex; align-items: center; gap: 10px; }
.focus-phase-label {
  font-size: 0.7rem; font-weight: 700; padding: 3px 10px; border-radius: 10px;
  text-transform: uppercase; letter-spacing: 0.5px;
}
.focus-phase-label.work  { background: rgba(48,127,226,0.2); color: #93C5FD; }
.focus-phase-label.short { background: rgba(16,185,129,0.2); color: #34D399; }
.focus-phase-label.long  { background: rgba(139,92,246,0.2); color: #A78BFA; }
.focus-time { font-size: 1.4rem; font-weight: 800; color: white; font-variant-numeric: tabular-nums; min-width: 64px; font-family: 'JetBrains Mono', monospace; }
.focus-timer-controls { display: flex; gap: 6px; }
.focus-btn-start, .focus-btn-pause, .focus-btn-reset {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 12px; border: none; border-radius: var(--radius-md);
  font-size: 0.78rem; font-weight: 600; cursor: pointer;
}
.focus-btn-start { background: var(--color-project); color: white; }
.focus-btn-start:hover { opacity: 0.88; }
.focus-btn-pause { background: var(--color-warning); color: white; }
.focus-btn-pause:hover { opacity: 0.88; }
.focus-btn-reset { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.5); padding: 6px 8px; }
.focus-btn-reset:hover { background: rgba(255,255,255,0.15); color: white; }
.focus-pomo-count { font-size: 0.7rem; color: rgba(255,255,255,0.3); white-space: nowrap; display: flex; align-items: center; gap: 4px; }
.pomo-dots { display: flex; gap: 2px; }
.pomo-dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.1); }
.pomo-dot.filled { background: var(--color-project); }

.focus-btn-exit {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: rgba(255,255,255,0.06);
  border: 1px solid rgba(255,255,255,0.1); border-radius: var(--radius-md);
  color: rgba(255,255,255,0.4); font-size: 0.8rem; font-weight: 600; cursor: pointer;
  transition: all var(--transition-fast); white-space: nowrap;
}
.focus-btn-exit:hover { background: rgba(239,68,68,0.12); color: #FCA5A5; border-color: rgba(239,68,68,0.3); }

/* 바디 */
.focus-body { flex: 1; display: flex; align-items: center; justify-content: center; overflow-y: auto; padding: 32px; }
.focus-content { display: flex; flex-direction: column; align-items: center; gap: 32px; width: 100%; max-width: 520px; }

/* 현재 태스크 카드 */
.focus-current-task {
  width: 100%; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
  border-radius: var(--radius-xl); padding: 20px 24px; display: flex; align-items: flex-start; gap: 14px;
}
.fct-priority-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.fct-info { flex: 1; }
.fct-title { font-size: 1.05rem; font-weight: 700; color: white; margin-bottom: 6px; line-height: 1.4; }
.fct-desc { font-size: 0.825rem; color: rgba(255,255,255,0.4); margin-bottom: 10px; line-height: 1.5; }
.fct-meta { display: flex; gap: 12px; }
.fct-due, .fct-hours { display: flex; align-items: center; gap: 4px; font-size: 0.75rem; color: rgba(255,255,255,0.3); }
.btn-complete-task {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  padding: 8px 16px; background: var(--color-success); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.825rem; font-weight: 700;
  cursor: pointer; transition: opacity var(--transition-fast);
}
.btn-complete-task:hover { opacity: 0.88; }

/* 타이머 링 */
.focus-ring-wrap { position: relative; width: 200px; height: 200px; }
.focus-ring { width: 100%; height: 100%; transform: rotate(-90deg); }
.ring-bg { fill: none; stroke: rgba(255,255,255,0.06); stroke-width: 8; }
.ring-fill { fill: none; stroke-width: 8; transition: stroke-dasharray 0.5s ease; }
.ring-fill.work  { stroke: var(--color-project); }
.ring-fill.short { stroke: var(--color-success); }
.ring-fill.long  { stroke: #8B5CF6; }
.ring-inner {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 4px;
}
.ring-time { font-size: 2.2rem; font-weight: 800; color: white; font-variant-numeric: tabular-nums; font-family: 'JetBrains Mono', monospace; }
.ring-phase { font-size: 0.78rem; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; }
.ring-phase.work  { color: #93C5FD; }
.ring-phase.short { color: #34D399; }
.ring-phase.long  { color: #A78BFA; }

/* 태스크 목록 */
.focus-task-list { width: 100%; }
.task-list-label { font-size: 0.72rem; font-weight: 700; color: rgba(255,255,255,0.3); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }
.focus-task-item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 14px; border-radius: var(--radius-md); cursor: pointer;
  border: 1px solid transparent; transition: all var(--transition-fast); margin-bottom: 6px;
  background: rgba(255,255,255,0.03);
}
.focus-task-item:hover { background: rgba(255,255,255,0.06); }
.focus-task-item.selected { background: rgba(48,127,226,0.15); border-color: rgba(48,127,226,0.3); }
.fti-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.fti-info { flex: 1; }
.fti-title { font-size: 0.875rem; font-weight: 600; color: rgba(255,255,255,0.85); }
.fti-kpi { display: flex; align-items: center; gap: 3px; font-size: 0.72rem; color: #93C5FD; margin-top: 3px; }
.fti-check { color: var(--color-project); flex-shrink: 0; }
.focus-no-tasks { text-align: center; color: rgba(255,255,255,0.3); font-size: 0.875rem; line-height: 1.7; }

/* confetti */
.confetti-wrap { position: fixed; inset: 0; pointer-events: none; overflow: hidden; z-index: 9999; }
.confetti-piece {
  position: absolute; top: -10px; width: 8px; height: 16px;
  border-radius: 2px; opacity: 0.9;
  animation: confetti-fall 2s ease-in forwards;
}
@keyframes confetti-fall {
  0%   { transform: translateY(0) rotate(0deg);   opacity: 1; }
  100% { transform: translateY(105vh) rotate(720deg); opacity: 0; }
}
</style>
