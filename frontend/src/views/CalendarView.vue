<template>
  <div class="page-wrapper">
    <div class="page-container" :class="{ 'panel-open': !!selectedDate }">

      <!-- 월 헤더 -->
      <div class="cal-header">
        <div class="cal-nav">
          <el-button circle @click="changeMonth(-1)"><ChevronLeft :size="16"/></el-button>
          <span class="cal-title">{{ dayjs(viewDate).format('YYYY년 MM월') }}</span>
          <el-button circle @click="changeMonth(1)" :disabled="isMaxFutureMonth">
            <ChevronRight :size="16"/>
          </el-button>
          <el-button size="small" plain @click="goCurrentMonth" v-if="!isCurrentMonth">이번 달</el-button>
        </div>
        <div class="cal-legend">
          <span class="leg-item"><span class="leg-dot" style="background:#DCFCE7"></span>높음(80%+)</span>
          <span class="leg-item"><span class="leg-dot" style="background:#FEF9C3"></span>보통(50~79%)</span>
          <span class="leg-item"><span class="leg-dot" style="background:#FEE2E2"></span>낮음(~49%)</span>
          <span class="leg-item"><span class="leg-dot leg-scrum"></span>스크럼</span>
        </div>
      </div>

      <!-- 월간 통계 -->
      <div class="month-stats" v-if="!loading">
        <div class="mstat">
          <div class="mstat-val">{{ monthSummary.recordedDays }}일</div>
          <div class="mstat-label">KPI 기록일</div>
        </div>
        <div class="mstat">
          <div class="mstat-val">{{ monthSummary.totalRecords }}건</div>
          <div class="mstat-label">총 KPI 기록</div>
        </div>
        <div class="mstat">
          <div class="mstat-val" :style="{ color: rateColor(monthSummary.avgRate) }">
            {{ monthSummary.avgRate }}%
          </div>
          <div class="mstat-label">평균 달성률</div>
        </div>
        <div class="mstat">
          <div class="mstat-val">{{ scrumMonthSummary.activeDays }}일</div>
          <div class="mstat-label">스크럼 기록일</div>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="loading-center">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      </div>

      <!-- 달력 그리드 -->
      <div class="calendar-wrap" v-else>
        <div class="cal-grid">
          <div v-for="d in weekDays" :key="d" class="cal-weekday">{{ d }}</div>
          <div v-for="i in startPadding" :key="'pad-' + i" class="cal-cell empty"></div>

          <div
            v-for="day in daysInMonth"
            :key="day"
            class="cal-cell"
            :class="cellClass(day)"
            :style="cellStyle(day)"
            @click="selectDay(day)"
          >
            <div class="cal-day-num">{{ day }}</div>

            <div v-if="getDayData(day)" class="cal-day-content">
              <div class="cal-rate">{{ getDayData(day).rate }}%</div>
              <div class="cal-rec-count">{{ getDayData(day).count }}건</div>
            </div>
            <div v-else-if="isPast(day) || isToday(day)" class="cal-no-data">
              <Minus :size="10" />
            </div>

            <!-- 스크럼 배지 -->
            <div v-if="getScrumData(day)" class="cal-scrum-badge">
              <CheckSquare :size="9" />
              <span>{{ getScrumData(day).done }}/{{ getScrumData(day).total }}</span>
            </div>

            <!-- 일정 배지 -->
            <div v-if="getSchedules(day).length" class="cal-schedule-badge">
              <span class="schedule-dot"></span>
              <span>{{ getSchedules(day).length }}개 일정</span>
            </div>

            <div v-if="isToday(day)" class="today-dot"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── 사이드 패널 오버레이 ── -->
    <Transition name="panel">
      <div v-if="selectedDate" class="side-panel">

        <!-- 패널 헤더 -->
        <div class="panel-header">
          <div class="panel-date">
            <CalendarDays :size="16" class="panel-date-icon" />
            <div>
              <div class="panel-date-main">{{ panelDateLabel }}</div>
              <div class="panel-date-sub">{{ panelWeekday }}</div>
            </div>
          </div>
          <button class="panel-close" @click="selectedDate = null">
            <X :size="18" />
          </button>
        </div>

        <!-- 패널 바디 (스크롤) -->
        <div class="panel-body">

          <!-- 일정 섹션 (항상 표시) -->
          <div v-if="panelSchedules.length" class="panel-section">
            <div class="panel-section-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="13" height="13" fill="#E01E5A" style="flex-shrink:0">
                <path d="M5.042 15.165a2.528 2.528 0 0 1-2.52 2.523A2.528 2.528 0 0 1 0 15.165a2.527 2.527 0 0 1 2.522-2.52h2.52v2.52zM6.313 15.165a2.527 2.527 0 0 1 2.521-2.52 2.527 2.527 0 0 1 2.521 2.52v6.313A2.528 2.528 0 0 1 8.834 24a2.528 2.528 0 0 1-2.521-2.522v-6.313zM8.834 5.042a2.528 2.528 0 0 1-2.521-2.52A2.528 2.528 0 0 1 8.834 0a2.528 2.528 0 0 1 2.521 2.522v2.52H8.834zM8.834 6.313a2.528 2.528 0 0 1 2.521 2.521 2.528 2.528 0 0 1-2.521 2.521H2.522A2.528 2.528 0 0 1 0 8.834a2.528 2.528 0 0 1 2.522-2.521h6.312zM18.956 8.834a2.528 2.528 0 0 1 2.522-2.521A2.528 2.528 0 0 1 24 8.834a2.528 2.528 0 0 1-2.522 2.521h-2.522V8.834zM17.688 8.834a2.528 2.528 0 0 1-2.523 2.521 2.527 2.527 0 0 1-2.52-2.521V2.522A2.527 2.527 0 0 1 15.165 0a2.528 2.528 0 0 1 2.523 2.522v6.312zM15.165 18.956a2.528 2.528 0 0 1 2.523 2.522A2.528 2.528 0 0 1 15.165 24a2.527 2.527 0 0 1-2.52-2.522v-2.522h2.52zM15.165 17.688a2.527 2.527 0 0 1-2.52-2.523 2.526 2.526 0 0 1 2.52-2.52h6.313A2.527 2.527 0 0 1 24 15.165a2.528 2.528 0 0 1-2.522 2.523h-6.313z"/>
              </svg>
              Slack 일정
            </div>
            <div v-for="s in panelSchedules" :key="s.id" class="panel-schedule-item">
              <div class="psi-info">
                <span class="psi-time" v-if="s.eventTime">{{ s.eventTime }}</span>
                <span class="psi-title">{{ s.title }}</span>
              </div>
              <button class="psi-del" @click="removeSchedule(s.id)" title="삭제">
                <X :size="12" />
              </button>
            </div>
          </div>

          <!-- 데이터 없음 -->
          <div v-if="!panelKpiData && !panelScrumData && !panelSchedules.length" class="panel-empty">
            <CalendarDays :size="32" class="panel-empty-icon" />
            <p>이 날의 기록이 없습니다.</p>
            <button class="panel-go-btn primary" @click="goToScrum">
              <Plus :size="14" /> 스크럼 작성하기
            </button>
          </div>

          <template v-else>

            <!-- ── 체크인 ── -->
            <div v-if="panelCheckIn && (panelCheckIn.energy || panelCheckIn.focus)" class="panel-section">
              <div class="section-label">체크인</div>
              <div class="checkin-row">
                <div v-if="panelCheckIn.energy" class="energy-row">
                  <Zap :size="13" class="energy-icon" />
                  <div class="energy-bars">
                    <span
                      v-for="n in 5" :key="n"
                      class="energy-pip"
                      :class="{ active: panelCheckIn.energy >= n }"
                    ></span>
                  </div>
                  <span class="energy-val">{{ panelCheckIn.energy }}/5</span>
                </div>
                <div v-if="panelCheckIn.focus" class="focus-row">
                  <Target :size="13" class="focus-icon" />
                  <span class="focus-text">{{ panelCheckIn.focus }}</span>
                </div>
              </div>
            </div>

            <!-- ── KPI 기록 ── -->
            <div v-if="panelKpiRecords.length" class="panel-section">
              <div class="section-label">
                <BarChart2 :size="13" />
                KPI 기록
                <span class="section-count">{{ panelKpiRecords.length }}건</span>
              </div>
              <div class="kpi-list">
                <div v-for="rec in panelKpiRecords" :key="rec.id" class="kpi-item">
                  <div class="kpi-item-left">
                    <span class="kpi-dot" :style="{ background: rec.categoryColor || '#94A3B8' }"></span>
                    <span class="kpi-item-name">{{ rec.kpiName }}</span>
                  </div>
                  <div class="kpi-item-right">
                    <span class="kpi-item-val">{{ rec.displayValue }}</span>
                    <div class="kpi-item-bar">
                      <div
                        class="kpi-item-fill"
                        :style="{
                          width: Math.min(rec.rate, 100) + '%',
                          background: rateColor(rec.rate)
                        }"
                      ></div>
                    </div>
                    <span class="kpi-item-rate" :style="{ color: rateColor(rec.rate) }">
                      {{ rec.rate }}%
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- ── 스크럼 태스크 ── -->
            <div v-if="panelScrumData" class="panel-section">
              <div class="section-label">
                <ClipboardList :size="13" />
                데일리 스크럼
                <span class="section-count">
                  {{ panelScrumData.done }}/{{ panelScrumData.total }} 완료
                </span>
              </div>

              <!-- 진행바 -->
              <div class="scrum-progress-bar">
                <div
                  class="scrum-progress-fill"
                  :style="{
                    width: Math.round(panelScrumData.done / panelScrumData.total * 100) + '%',
                    background: rateColor(Math.round(panelScrumData.done / panelScrumData.total * 100))
                  }"
                ></div>
              </div>

              <!-- 태스크 목록 -->
              <div class="task-list">
                <div
                  v-for="task in panelScrumData.tasks"
                  :key="task.id"
                  class="task-item"
                  :class="{ done: task.done }"
                >
                  <span class="task-check-icon">
                    <Check v-if="task.done" :size="11" />
                    <span v-else class="task-circle"></span>
                  </span>
                  <span class="task-name">{{ task.title }}</span>
                  <div class="task-meta">
                    <span v-if="task.priority" class="task-pri" :class="`pri-${task.priority.toLowerCase()}`">
                      {{ task.priority }}
                    </span>
                    <span v-if="task.estimatedHours" class="task-hours">
                      <Clock :size="10" /> {{ task.estimatedHours }}h
                    </span>
                  </div>
                </div>
              </div>

              <!-- 블로커 -->
              <div v-if="panelScrumData.blocker" class="blocker-box">
                <AlertTriangle :size="13" class="blocker-icon" />
                <span class="blocker-text">{{ panelScrumData.blocker }}</span>
              </div>
            </div>

          </template>
        </div>

        <!-- 패널 푸터 -->
        <div class="panel-footer">
          <button v-if="panelKpiData" class="panel-go-btn" @click="goToRecord">
            <BarChart2 :size="13" /> KPI 기록 보기
          </button>
          <button class="panel-go-btn primary" @click="goToScrum">
            <Zap :size="13" /> 스크럼으로 이동
          </button>
        </div>
      </div>
    </Transition>

    <!-- 오버레이 배경 (모바일) -->
    <Transition name="fade">
      <div v-if="selectedDate" class="overlay-bg" @click="selectedDate = null"></div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'
import { recordApi } from '@/api/kpiApi'
import { scheduleApi } from '@/api/scheduleApi'
import { useKpiStore } from '@/store/kpiStore'
import {
  ChevronLeft, ChevronRight, Minus, CheckSquare, CalendarDays,
  X, Zap, Target, BarChart2, ClipboardList, Check, Clock, AlertTriangle, Plus
} from 'lucide-vue-next'

dayjs.locale('ko')

const router  = useRouter()
const store   = useKpiStore()
const loading = ref(false)
const records = ref([])

const viewDate    = ref(dayjs().format('YYYY-MM'))
const selectedDate = ref(null)   // 'YYYY-MM-DD' 형식

const weekDays = ['일', '월', '화', '수', '목', '금', '토']

const isCurrentMonth = computed(() => viewDate.value === dayjs().format('YYYY-MM'))
const maxFutureMonth = dayjs().add(6, 'month').format('YYYY-MM')
const isMaxFutureMonth = computed(() => viewDate.value >= maxFutureMonth)
const daysInMonth    = computed(() => dayjs(viewDate.value + '-01').daysInMonth())
const startPadding   = computed(() => dayjs(viewDate.value + '-01').day())

// ── KPI 날짜별 집계 ──
const dayDataMap = computed(() => {
  const map    = {}
  const kpiMap = {}
  store.activeKpis.forEach(k => { kpiMap[k.id] = k })

  records.value.forEach(r => {
    const d = r.recordedDate
    if (!map[d]) map[d] = { count: 0, totalRate: 0, rateCount: 0 }
    map[d].count++
    const kpi = kpiMap[r.kpiId]
    if (!kpi) return
    let rate = 0
    if (kpi.kpiType === 'BOOLEAN') {
      rate = r.booleanValue ? 100 : 0
    } else if (kpi.targetValue && r.actualValue != null) {
      rate = Math.min(r.actualValue / kpi.targetValue * 100, 100)
    } else return
    map[d].totalRate += rate
    map[d].rateCount++
  })

  const result = {}
  Object.entries(map).forEach(([date, v]) => {
    result[date] = {
      count: v.count,
      rate:  v.rateCount > 0 ? Math.round(v.totalRate / v.rateCount) : 0
    }
  })
  return result
})

function getDayData(day) {
  return dayDataMap.value[`${viewDate.value}-${String(day).padStart(2, '0')}`] || null
}

// ── 스크럼 데이터 (localStorage) ──
const scrumDataMap = computed(() => {
  const map  = {}
  const days = dayjs(viewDate.value + '-01').daysInMonth()
  for (let d = 1; d <= days; d++) {
    const date = `${viewDate.value}-${String(d).padStart(2, '0')}`
    try {
      const raw = localStorage.getItem(`workoop-scrum-${date}`)
      if (!raw) continue
      const data = JSON.parse(raw)
      if (!data?.tasks?.length) continue
      map[date] = {
        total:   data.tasks.length,
        done:    data.tasks.filter(t => t.done).length,
        tasks:   data.tasks,
        blocker: data.blocker || ''
      }
    } catch { /* ignore */ }
  }
  return map
})

function getScrumData(day) {
  return scrumDataMap.value[`${viewDate.value}-${String(day).padStart(2, '0')}`] || null
}

const scrumMonthSummary = computed(() => ({
  activeDays: Object.keys(scrumDataMap.value).length
}))

// ── 일정(schedule) 데이터 ──
const schedules = ref([])

const scheduleMap = computed(() => {
  const map = {}
  schedules.value.forEach(s => {
    if (!map[s.eventDate]) map[s.eventDate] = []
    map[s.eventDate].push(s)
  })
  return map
})

function getSchedules(day) {
  return scheduleMap.value[`${viewDate.value}-${String(day).padStart(2, '0')}`] || []
}

const panelSchedules = computed(() =>
  selectedDate.value ? (scheduleMap.value[selectedDate.value] || []) : []
)

async function loadSchedules() {
  const from = `${viewDate.value}-01`
  const to   = dayjs(viewDate.value + '-01').endOf('month').format('YYYY-MM-DD')
  try {
    const res = await scheduleApi.list(from, to)
    schedules.value = res.data || []
  } catch (e) { console.error('일정 조회 실패', e) }
}

async function removeSchedule(id) {
  await scheduleApi.remove(id)
  schedules.value = schedules.value.filter(s => s.id !== id)
}

// ── 패널 데이터 ──
const panelKpiData  = computed(() => selectedDate.value ? dayDataMap.value[selectedDate.value] || null : null)
const panelScrumData = computed(() => selectedDate.value ? scrumDataMap.value[selectedDate.value] || null : null)

const panelCheckIn = computed(() => {
  if (!selectedDate.value) return null
  try {
    return JSON.parse(localStorage.getItem(`workoop-checkin-${selectedDate.value}`)) || null
  } catch { return null }
})

const panelKpiRecords = computed(() => {
  if (!selectedDate.value) return []
  const kpiMap = {}
  store.activeKpis.forEach(k => { kpiMap[k.id] = k })

  const catMap = {}
  ;(store.categories || []).forEach(c => { catMap[c.id] = c })

  return records.value
    .filter(r => r.recordedDate === selectedDate.value)
    .map(r => {
      const kpi = kpiMap[r.kpiId]
      const cat = kpi ? catMap[kpi.categoryId] : null
      let rate = 0
      let displayValue = '-'
      if (kpi?.kpiType === 'BOOLEAN') {
        rate = r.booleanValue ? 100 : 0
        displayValue = r.booleanValue ? '완료' : '미완료'
      } else if (kpi?.targetValue && r.actualValue != null) {
        rate = Math.round(Math.min(r.actualValue / kpi.targetValue * 100, 100))
        displayValue = `${r.actualValue} ${kpi.unit || ''}`
      }
      return {
        id:            r.id,
        kpiName:       kpi?.name || '알 수 없음',
        categoryColor: cat?.color || '#94A3B8',
        displayValue,
        rate
      }
    })
})

const panelDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  return dayjs(selectedDate.value).format('YYYY년 M월 D일')
})
const panelWeekday = computed(() => {
  if (!selectedDate.value) return ''
  return dayjs(selectedDate.value).format('dddd')
})

// ── 날짜 유틸 ──
function isPast(day) {
  return dayjs(`${viewDate.value}-${String(day).padStart(2, '0')}`).isBefore(dayjs(), 'day')
}
function isToday(day) {
  return `${viewDate.value}-${String(day).padStart(2, '0')}` === dayjs().format('YYYY-MM-DD')
}
function rateColor(rate) {
  if (rate >= 80) return '#15803D'
  if (rate >= 50) return '#B45309'
  return '#DC2626'
}

function cellClass(day) {
  const date = `${viewDate.value}-${String(day).padStart(2, '0')}`
  return {
    'has-data':  !!getDayData(day),
    'has-scrum': !!getScrumData(day),
    'is-today':  isToday(day),
    'is-past':   isPast(day),
    'selected':  selectedDate.value === date
  }
}
function cellStyle(day) {
  const data = getDayData(day)
  if (!data) return {}
  const r = data.rate
  if (r >= 80) return { background: '#DCFCE7', borderColor: '#86EFAC' }
  if (r >= 50) return { background: '#FEF9C3', borderColor: '#FDE68A' }
  return { background: '#FEE2E2', borderColor: '#FECACA' }
}

// ── 액션 ──
function selectDay(day) {
  const date = `${viewDate.value}-${String(day).padStart(2, '0')}`
  selectedDate.value = selectedDate.value === date ? null : date
}
function goToRecord() {
  router.push({ path: '/records', query: { date: selectedDate.value } })
}
function goToScrum() {
  router.push({ path: '/scrum', query: { date: selectedDate.value } })
}

// ── 월간 KPI 통계 ──
const monthSummary = computed(() => {
  const days = Object.values(dayDataMap.value)
  if (!days.length) return { recordedDays: 0, totalRecords: 0, avgRate: 0 }
  return {
    recordedDays: days.length,
    totalRecords: days.reduce((s, d) => s + d.count, 0),
    avgRate:      Math.round(days.reduce((s, d) => s + d.rate, 0) / days.length)
  }
})

async function loadData() {
  loading.value = true
  try {
    const start = `${viewDate.value}-01`
    const end   = dayjs(viewDate.value + '-01').endOf('month').format('YYYY-MM-DD')
    await store.fetchKpis()
    if (store.categories == null) await store.fetchCategories?.()
    const res   = await recordApi.getByDateRange(start, end)
    records.value = res.data || []
  } finally {
    loading.value = false
  }
  loadSchedules()
}

function changeMonth(delta) {
  viewDate.value  = dayjs(viewDate.value + '-01').add(delta, 'month').format('YYYY-MM')
  selectedDate.value = null
  loadData()
}
function goCurrentMonth() {
  viewDate.value  = dayjs().format('YYYY-MM')
  selectedDate.value = null
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
/* ── 레이아웃 래퍼 ── */
.page-wrapper {
  display: flex;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.page-container {
  flex: 1;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  transition: margin-right 0.3s ease;
}
.page-container.panel-open {
  margin-right: 360px;
}

/* ── 헤더 ── */
.cal-header {
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 14px 20px; box-shadow: var(--shadow-xs);
}
.cal-nav { display: flex; align-items: center; gap: 10px; }
.cal-title { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); min-width: 130px; text-align: center; }
.cal-legend { display: flex; gap: 12px; flex-wrap: wrap; }
.leg-item { display: flex; align-items: center; gap: 4px; font-size: 0.75rem; color: var(--text-secondary); }
.leg-dot { width: 12px; height: 12px; border-radius: 3px; border: 1px solid var(--border-color); }
.leg-scrum { background: rgba(139,92,246,0.15); border-color: rgba(139,92,246,0.35); }

/* ── 통계 ── */
.month-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.mstat {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 14px 16px; text-align: center; box-shadow: var(--shadow-xs);
}
.mstat-val { font-size: 1.4rem; font-weight: 800; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.mstat-label { font-size: 0.72rem; color: var(--text-muted); margin-top: 3px; }
.loading-center { display: flex; justify-content: center; align-items: center; height: 260px; }

/* ── 달력 ── */
.calendar-wrap {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 18px; box-shadow: var(--shadow-xs);
}
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 5px; }
.cal-weekday {
  text-align: center; font-size: 0.76rem; font-weight: 700; color: var(--text-muted); padding: 7px 0;
}
.cal-weekday:first-child { color: var(--color-danger); }
.cal-weekday:last-child  { color: var(--color-project); }

.cal-cell {
  min-height: 78px; border: 1.5px solid var(--border-color); border-radius: var(--radius-md);
  padding: 7px; cursor: pointer; transition: all var(--transition-fast);
  position: relative; background: var(--bg-card);
}
.cal-cell:hover:not(.empty) { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.cal-cell.empty  { background: transparent; border: none; cursor: default; }
.cal-cell.is-today  { border-color: var(--color-project) !important; }
.cal-cell.selected  { box-shadow: 0 0 0 2px var(--color-project); background: rgba(48,127,226,0.06) !important; }
.cal-cell.has-scrum { border-bottom: 2.5px solid #A78BFA; }

.cal-day-num { font-size: 0.8rem; font-weight: 700; color: var(--text-primary); margin-bottom: 3px; }
.cal-cell.is-today .cal-day-num { color: var(--color-project); }
.cal-day-content { display: flex; flex-direction: column; gap: 1px; }
.cal-rate { font-size: 0.8rem; font-weight: 800; font-family: 'JetBrains Mono', monospace; }
.cal-rec-count { font-size: 0.65rem; color: var(--text-muted); }
.cal-no-data { color: var(--border-color); display: flex; padding-top: 3px; }

.cal-scrum-badge {
  display: flex; align-items: center; gap: 2px;
  margin-top: 4px; font-size: 0.63rem; font-weight: 700;
  color: #7C3AED; background: rgba(139,92,246,0.12); border-radius: var(--radius-xs); padding: 1px 4px; width: fit-content;
}
.today-dot {
  position: absolute; top: 5px; right: 5px;
  width: 5px; height: 5px; border-radius: 50%; background: var(--color-project);
}
.cal-schedule-badge {
  display: flex; align-items: center; gap: 3px;
  margin-top: 3px; font-size: 0.62rem; font-weight: 600;
  color: #E01E5A; background: rgba(224,30,90,0.1); border-radius: 3px; padding: 1px 4px; width: fit-content;
}
.schedule-dot { width: 5px; height: 5px; border-radius: 50%; background: #E01E5A; flex-shrink: 0; }

/* ── 패널 일정 ── */
.panel-schedule-item {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(224,30,90,0.07); border: 1px solid rgba(224,30,90,0.2);
  border-radius: 7px; padding: 8px 10px;
}
.psi-info { display: flex; align-items: center; gap: 8px; min-width: 0; }
.psi-time { font-size: 11px; font-weight: 700; color: #E01E5A; flex-shrink: 0; }
.psi-title { font-size: 12px; color: #E2E8F0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.psi-del {
  background: none; border: none; cursor: pointer; color: #718096;
  padding: 2px; border-radius: 4px; flex-shrink: 0; display: flex; align-items: center;
}
.psi-del:hover { color: #EF4444; background: rgba(239,68,68,0.1); }

/* ══════════════════════════════
   사이드 패널
══════════════════════════════ */
.side-panel {
  position: fixed;
  top: 0; right: 0;
  width: 360px;
  height: 100%;
  background: var(--bg-card);
  border-left: 1px solid var(--border-color);
  display: flex; flex-direction: column;
  z-index: 100;
  box-shadow: var(--shadow-xl);
}

/* 트랜지션 */
.panel-enter-active, .panel-leave-active { transition: transform .28s cubic-bezier(.4,0,.2,1); }
.panel-enter-from, .panel-leave-to { transform: translateX(100%); }

.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 패널 헤더 */
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}
.panel-date { display: flex; align-items: center; gap: 10px; }
.panel-date-icon { color: var(--color-project); flex-shrink: 0; }
.panel-date-main { font-size: 1rem; font-weight: 800; color: var(--text-primary); }
.panel-date-sub   { font-size: 0.78rem; color: var(--text-muted); margin-top: 1px; }
.panel-close {
  width: 30px; height: 30px; border: none; background: var(--bg-hover);
  border-radius: var(--radius-md); cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: all var(--transition-fast);
}
.panel-close:hover { background: var(--border-color); color: var(--text-primary); }

/* 패널 바디 */
.panel-body {
  flex: 1; overflow-y: auto; padding: 20px;
  display: flex; flex-direction: column; gap: 20px;
}

.panel-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; height: 100%; color: var(--text-muted); text-align: center;
}
.panel-empty-icon { color: var(--border-color); }
.panel-empty p { font-size: 0.88rem; }

/* 섹션 */
.panel-section { display: flex; flex-direction: column; gap: 10px; }
.section-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 0.72rem; font-weight: 700; color: var(--text-secondary);
  text-transform: uppercase; letter-spacing: 0.06em;
}
.section-count {
  margin-left: auto; font-size: 0.72rem; font-weight: 600;
  color: var(--color-project); background: rgba(48,127,226,0.08);
  padding: 1px 7px; border-radius: 999px;
}

/* 체크인 */
.checkin-row { display: flex; flex-direction: column; gap: 8px; }
.energy-row  { display: flex; align-items: center; gap: 8px; }
.energy-icon { color: var(--color-warning); }
.energy-bars { display: flex; gap: 3px; }
.energy-pip {
  width: 14px; height: 14px; border-radius: 3px; background: var(--bg-hover); transition: background var(--transition-fast);
}
.energy-pip.active { background: var(--color-warning); }
.energy-val { font-size: 0.78rem; color: var(--text-secondary); }

.focus-row  { display: flex; align-items: flex-start; gap: 8px; }
.focus-icon { color: var(--color-project); flex-shrink: 0; margin-top: 1px; }
.focus-text { font-size: 0.85rem; color: var(--text-primary); }

/* KPI 목록 */
.kpi-list { display: flex; flex-direction: column; gap: 8px; }
.kpi-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: var(--bg-hover); border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}
.kpi-item-left  { display: flex; align-items: center; gap: 7px; flex: 1; min-width: 0; }
.kpi-dot        { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.kpi-item-name  { font-size: 0.82rem; font-weight: 600; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kpi-item-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.kpi-item-val   { font-size: 0.75rem; color: var(--text-secondary); white-space: nowrap; }
.kpi-item-bar   { width: 48px; height: 4px; background: var(--border-color); border-radius: 999px; overflow: hidden; }
.kpi-item-fill  { height: 100%; border-radius: 999px; }
.kpi-item-rate  { font-size: 0.78rem; font-weight: 700; min-width: 34px; text-align: right; font-family: 'JetBrains Mono', monospace; }

/* 스크럼 태스크 */
.scrum-progress-bar {
  height: 5px; background: var(--bg-hover); border-radius: 999px; overflow: hidden;
}
.scrum-progress-fill { height: 100%; border-radius: 999px; transition: width .3s; }

.task-list { display: flex; flex-direction: column; gap: 6px; }
.task-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: var(--radius-md);
  background: var(--bg-hover); border: 1px solid var(--border-color);
  transition: opacity var(--transition-fast);
}
.task-item.done { opacity: 0.55; }

.task-check-icon {
  display: flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 4px;
  background: var(--border-color); flex-shrink: 0;
  font-size: 0.7rem; color: var(--color-success);
}
.task-item.done .task-check-icon { background: rgba(16,185,129,0.18); }
.task-circle { width: 8px; height: 8px; border-radius: 50%; background: var(--border-color); display: block; }

.task-name { flex: 1; font-size: 0.82rem; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-item.done .task-name { text-decoration: line-through; color: var(--text-muted); }

.task-meta  { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.task-pri   { font-size: 0.62rem; font-weight: 700; border-radius: var(--radius-xs); padding: 1px 5px; }
.pri-p1 { background: rgba(220,38,38,0.12); color: var(--color-danger); }
.pri-p2 { background: rgba(217,119,6,0.12); color: #D97706; }
.pri-p3 { background: rgba(2,132,199,0.12); color: #0284C7; }
.task-hours { display: flex; align-items: center; gap: 2px; font-size: 0.68rem; color: var(--text-muted); }

/* 블로커 */
.blocker-box {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 10px 12px; background: rgba(249,115,22,0.06); border: 1px solid rgba(249,115,22,0.25);
  border-radius: var(--radius-md);
}
.blocker-icon { color: #F97316; flex-shrink: 0; margin-top: 1px; }
.blocker-text { font-size: 0.82rem; color: var(--text-primary); }

/* 패널 푸터 */
.panel-footer {
  display: flex; gap: 8px; padding: 14px 20px;
  border-top: 1px solid var(--border-color); flex-shrink: 0;
}
.panel-go-btn {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 9px 14px; border-radius: var(--radius-md); cursor: pointer;
  font-size: 0.82rem; font-weight: 600; border: 1px solid var(--border-color);
  background: var(--bg-card); color: var(--text-secondary); transition: all var(--transition-fast);
}
.panel-go-btn:hover { background: var(--bg-hover); }
.panel-go-btn.primary { background: var(--color-project); color: white; border-color: var(--color-project); }
.panel-go-btn.primary:hover { opacity: 0.88; }

/* 모바일 오버레이 배경 */
.overlay-bg {
  display: none;
  position: fixed; inset: 0; background: rgba(17,24,39,0.35); z-index: 99;
}

@media (max-width: 768px) {
  .page-container.panel-open { margin-right: 0; }
  .side-panel { width: 100%; }
  .overlay-bg { display: block; }
  .month-stats { grid-template-columns: repeat(2, 1fr); }
  .cal-cell { min-height: 58px; }
}
</style>
