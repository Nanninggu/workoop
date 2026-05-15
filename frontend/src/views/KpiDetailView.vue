<template>
  <div class="page-container">
    <div v-if="loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <template v-else-if="kpi">
      <!-- Breadcrumb -->
      <div class="detail-breadcrumb">
        <router-link to="/kpis" class="bc-link">KPI 관리</router-link>
        <span class="bc-sep">›</span>
        <span class="bc-current">{{ kpi.name }}</span>
      </div>

      <!-- 헤더 -->
      <div class="detail-header">
        <div class="header-left">
          <button class="back-btn" @click="router.back()">
            <ArrowLeft :size="18" />
          </button>
          <div>
            <div class="detail-kpi-name">{{ kpi.name }}</div>
            <div class="detail-kpi-meta">
              <span class="meta-cat" :style="{ background: kpi.category?.color+'20', color: kpi.category?.color }">
                {{ kpi.category?.name }}
              </span>
              <span class="meta-chip">{{ frequencyLabel(kpi.frequency) }}</span>
              <span class="meta-chip">{{ kpiTypeLabel(kpi.kpiType) }}</span>
              <el-tag :type="statusTagType(kpi.status)" size="small">{{ statusLabel(kpi.status) }}</el-tag>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button @click="editDialogVisible = true">수정</el-button>
        </div>
      </div>

      <!-- 목표 정보 -->
      <div class="info-cards">
        <div class="info-card" v-if="kpi.kpiType !== 'BOOLEAN'">
          <div class="info-label">목표값</div>
          <div class="info-value">{{ kpi.targetValue?.toLocaleString() }} {{ kpi.unit }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">시작일</div>
          <div class="info-value">{{ kpi.startDate }}</div>
        </div>
        <div class="info-card" v-if="kpi.endDate">
          <div class="info-label">종료일</div>
          <div class="info-value">{{ kpi.endDate }}</div>
        </div>
        <div class="info-card">
          <div class="info-label">누적 기록</div>
          <div class="info-value">{{ records.length }}회</div>
        </div>
      </div>

      <!-- 기간 선택 -->
      <div class="period-selector">
        <el-button
          v-for="p in periods"
          :key="p.value"
          :type="selectedPeriod === p.value ? 'primary' : ''"
          size="small"
          @click="changePeriod(p.value)"
        >{{ p.label }}</el-button>
      </div>

      <!-- 차트: NUMERIC/PERCENTAGE -->
      <div class="chart-section" v-if="kpi.kpiType !== 'BOOLEAN'">
        <div class="chart-title">실적 추이</div>
        <v-chart :option="chartOption" style="height: 300px; width: 100%" autoresize />
      </div>

      <!-- 차트: BOOLEAN 달성 현황 (4번) -->
      <div class="chart-section" v-else>
        <!-- 달성 통계 요약 -->
        <div class="bool-summary-row">
          <div class="bool-stat achieved">
            <div class="bool-stat-value">{{ boolStats.achieved }}</div>
            <div class="bool-stat-label">달성</div>
          </div>
          <div class="bool-stat not-achieved">
            <div class="bool-stat-value">{{ boolStats.notAchieved }}</div>
            <div class="bool-stat-label">미달성</div>
          </div>
          <div class="bool-stat rate">
            <div class="bool-stat-value">{{ boolStats.rate }}%</div>
            <div class="bool-stat-label">달성률</div>
          </div>
        </div>
        <!-- 달성 현황 차트 -->
        <div class="chart-title" style="margin-top: 16px;">달성 현황</div>
        <v-chart
          v-if="records.length > 0"
          :option="boolChartOption"
          style="height: 220px; width: 100%"
          autoresize
        />
        <div v-else class="no-data-msg">기록된 데이터가 없습니다.</div>
      </div>

      <!-- 연간 달성 히트맵 (GitHub 잔디) -->
      <div class="chart-section heatmap-section">
        <div class="heatmap-header">
          <div class="chart-title">{{ dayjs().year() }}년 달성 현황</div>
          <div class="heatmap-legend">
            <span class="legend-label">낮음</span>
            <span class="legend-cell" style="background:#ebedf0"></span>
            <span class="legend-cell" style="background:#9be9a8"></span>
            <span class="legend-cell" style="background:#40c463"></span>
            <span class="legend-cell" style="background:#30a14e"></span>
            <span class="legend-cell" style="background:#216e39"></span>
            <span class="legend-label">높음</span>
          </div>
        </div>
        <div v-if="yearRecords.length === 0" class="no-data-msg">올해 기록된 데이터가 없습니다.</div>
        <v-chart
          v-else
          :option="calendarHeatmapOption"
          style="height: 175px; width: 100%"
          autoresize
        />
      </div>

      <!-- 마일스톤 (CAT7) -->
      <div class="chart-section milestone-section">
        <div class="milestone-header">
          <div class="chart-title" style="display:flex;align-items:center;gap:6px"><Target :size="15" /> 마일스톤</div>
          <button class="ms-add-btn" @click="showMilestoneForm = !showMilestoneForm">+ 추가</button>
        </div>

        <!-- 마일스톤 추가 폼 -->
        <div v-if="showMilestoneForm" class="ms-form">
          <el-input v-model="newMs.label" placeholder="마일스톤 이름" size="small" style="width:180px" />
          <el-input-number v-model="newMs.target" :min="0" size="small" placeholder="목표값" v-if="kpi.kpiType !== 'BOOLEAN'" style="width:120px" />
          <el-date-picker v-model="newMs.dueDate" type="date" value-format="YYYY-MM-DD" size="small" placeholder="목표일" style="width:140px" />
          <el-button size="small" type="primary" @click="addMilestone">저장</el-button>
          <el-button size="small" @click="showMilestoneForm = false">취소</el-button>
        </div>

        <!-- 마일스톤 목록 -->
        <div v-if="milestones.length > 0" class="ms-list">
          <div v-for="ms in milestones" :key="ms.id" class="ms-item" :class="{ done: ms.done }">
            <div class="ms-check" @click="toggleMilestone(ms)">
              <CheckSquare v-if="ms.done" :size="17" color="#10B981" />
              <Square v-else :size="17" color="#CBD5E1" />
            </div>
            <div class="ms-info">
              <div class="ms-label">{{ ms.label }}</div>
              <div class="ms-meta">
                <span v-if="ms.target && kpi.kpiType !== 'BOOLEAN'">목표: {{ ms.target }} {{ kpi.unit }}</span>
                <span v-if="ms.dueDate">📅 {{ ms.dueDate }}</span>
              </div>
            </div>
            <button class="ms-del" @click="deleteMilestone(ms.id)">✕</button>
          </div>
        </div>
        <div v-else class="no-data-msg">마일스톤을 추가해 KPI를 단계별로 관리하세요.</div>
      </div>

      <!-- 기록 테이블 -->
      <div class="records-section">
        <div class="flex items-center justify-between mb-3">
          <h3 class="section-title">기록 내역</h3>
        </div>
        <el-table :data="records" stripe :empty-text="'기록이 없습니다'" size="default">
          <el-table-column prop="recordedDate" label="날짜" width="130" sortable />
          <el-table-column label="실적" width="160">
            <template #default="{ row }">
              <span v-if="kpi.kpiType === 'BOOLEAN'">
                <el-tag :type="row.booleanValue ? 'success' : 'info'" size="small">
                  {{ row.booleanValue ? '달성' : '미달성' }}
                </el-tag>
              </span>
              <span v-else class="font-semibold">
                {{ row.actualValue?.toLocaleString() }} {{ kpi.unit }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="달성률" width="120" v-if="kpi.kpiType !== 'BOOLEAN'">
            <template #default="{ row }">
              <span :class="achievementClass(row)">
                {{ calcRate(row) }}%
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="note" label="메모" />
          <el-table-column width="80" align="center">
            <template #default="{ row }">
              <el-button
                text
                type="danger"
                size="small"
                @click="deleteRecord(row.id)"
              >삭제</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 수정 다이얼로그 -->
    <el-dialog v-model="editDialogVisible" title="KPI 수정" width="560px" destroy-on-close>
      <KpiForm
        :kpi="kpi"
        :categories="store.categories"
        @submit="onEditSubmit"
        @cancel="editDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useKpiStore } from '@/store/kpiStore'
import { kpiApi, recordApi } from '@/api/kpiApi'
import { ElMessageBox, ElMessage } from 'element-plus'
import KpiForm from '@/components/kpi/KpiForm.vue'
import { ArrowLeft, Target, CheckSquare, Square } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { LineChart, BarChart, HeatmapChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, MarkLineComponent,
  CalendarComponent, VisualMapComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'

use([
  LineChart, BarChart, HeatmapChart,
  GridComponent, TooltipComponent, LegendComponent, MarkLineComponent,
  CalendarComponent, VisualMapComponent,
  CanvasRenderer
])

const route = useRoute()
const router = useRouter()
const store = useKpiStore()

const kpi = ref(null)
const records = ref([])
const yearRecords = ref([])
const loading = ref(false)
const editDialogVisible = ref(false)
const selectedPeriod = ref(30)

const periods = [
  { label: '2주', value: 14 },
  { label: '1개월', value: 30 },
  { label: '3개월', value: 90 },
  { label: '6개월', value: 180 }
]

const chartOption = computed(() => {
  const sorted = [...records.value].sort((a, b) => a.recordedDate.localeCompare(b.recordedDate))
  const dates = sorted.map(r => r.recordedDate)
  const values = sorted.map(r => r.actualValue)

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 30, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: kpi.value?.unit || '',
      nameTextStyle: { fontSize: 11 }
    },
    series: [
      {
        name: kpi.value?.name,
        type: 'line',
        data: values,
        smooth: true,
        lineStyle: { color: '#3B82F6', width: 3 },
        itemStyle: { color: '#3B82F6' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59,130,246,0.3)' },
              { offset: 1, color: 'rgba(59,130,246,0.02)' }
            ]
          }
        },
        markLine: kpi.value?.targetValue ? {
          data: [{ yAxis: kpi.value.targetValue, name: '목표' }],
          lineStyle: { color: '#F59E0B', type: 'dashed' },
          label: { formatter: '목표: {c}', fontSize: 11 }
        } : undefined
      }
    ]
  }
})

// BOOLEAN 달성 통계 (4번)
const boolStats = computed(() => {
  const achieved = records.value.filter(r => r.booleanValue === true).length
  const notAchieved = records.value.filter(r => r.booleanValue === false).length
  const total = achieved + notAchieved
  return {
    achieved,
    notAchieved,
    rate: total === 0 ? 0 : Math.round(achieved / total * 100)
  }
})

// BOOLEAN 차트 옵션 (4번)
const boolChartOption = computed(() => {
  const sorted = [...records.value].sort((a, b) => a.recordedDate.localeCompare(b.recordedDate))
  const dates = sorted.map(r => r.recordedDate)
  const data = sorted.map(r => ({
    value: 1,
    itemStyle: {
      color: r.booleanValue ? '#10B981' : '#FCA5A5',
      borderRadius: [4, 4, 0, 0]
    },
    label: {
      show: true,
      position: 'top',
      formatter: r.booleanValue ? '✓' : '✗',
      fontSize: 13,
      color: r.booleanValue ? '#059669' : '#DC2626',
      fontWeight: 'bold'
    }
  }))

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const r = sorted[params[0].dataIndex]
        return `${r.recordedDate}<br/>${r.booleanValue ? '<b style="color:#10B981">달성</b>' : '<b style="color:#EF4444">미달성</b>'}`
      }
    },
    grid: { left: 30, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { rotate: 30, fontSize: 10, color: '#94A3B8' },
      axisLine: { lineStyle: { color: '#E2E8F0' } }
    },
    yAxis: {
      type: 'value',
      min: 0, max: 1.5,
      show: false
    },
    series: [{
      type: 'bar',
      data,
      barMaxWidth: 32
    }]
  }
})

async function loadKpi() {
  loading.value = true
  try {
    const res = await kpiApi.getById(route.params.id)
    kpi.value = res.data
    await store.fetchCategories()
    await Promise.all([loadRecords(), loadYearRecords()])
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  const end = dayjs().format('YYYY-MM-DD')
  const start = dayjs().subtract(selectedPeriod.value, 'day').format('YYYY-MM-DD')
  const res = await recordApi.getByKpiId(route.params.id, { startDate: start, endDate: end })
  records.value = (res.data || []).sort((a, b) => b.recordedDate.localeCompare(a.recordedDate))
}

async function loadYearRecords() {
  const yearStart = dayjs().startOf('year').format('YYYY-MM-DD')
  const yearEnd   = dayjs().format('YYYY-MM-DD')
  const res = await recordApi.getByKpiId(route.params.id, { startDate: yearStart, endDate: yearEnd })
  yearRecords.value = res.data || []
}

function changePeriod(val) {
  selectedPeriod.value = val
  loadRecords()
}

async function deleteRecord(id) {
  await ElMessageBox.confirm('이 기록을 삭제하시겠습니까?', '삭제 확인', {
    confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning'
  })
  await recordApi.delete(id)
  ElMessage.success('삭제되었습니다.')
  await Promise.all([loadRecords(), loadYearRecords()])
}

async function onEditSubmit(data) {
  await store.updateKpi(kpi.value.id, data)
  editDialogVisible.value = false
  await loadKpi()
}

// 연간 달성 히트맵 (GitHub 잔디 스타일)
const calendarHeatmapOption = computed(() => {
  const year = dayjs().year()
  const rateMap = {}
  yearRecords.value.forEach(r => {
    if (kpi.value?.kpiType === 'BOOLEAN') {
      rateMap[r.recordedDate] = r.booleanValue ? 100 : 10
    } else if (kpi.value?.targetValue && r.actualValue != null) {
      rateMap[r.recordedDate] = Math.min(
        Math.round(r.actualValue / kpi.value.targetValue * 100), 100
      )
    }
  })
  const data = Object.entries(rateMap).map(([date, rate]) => [date, rate])

  return {
    tooltip: {
      formatter: (p) => {
        const rate = p.data[1]
        const label = kpi.value?.kpiType === 'BOOLEAN'
          ? (rate === 100 ? '달성' : '미달성')
          : `달성률 ${rate}%`
        return `${p.data[0]}<br/><b>${label}</b>`
      }
    },
    visualMap: {
      min: 0, max: 100,
      show: false,
      inRange: { color: ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'] }
    },
    calendar: {
      top: 30, left: 40, right: 20,
      range: String(year),
      cellSize: ['auto', 14],
      dayLabel: {
        nameMap: ['일', '월', '화', '수', '목', '금', '토'],
        fontSize: 10, color: '#94A3B8', firstDay: 0
      },
      monthLabel: {
        nameMap: ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
        fontSize: 11, color: '#64748B'
      },
      yearLabel: { show: false },
      itemStyle: { borderWidth: 3, borderColor: '#fff', borderRadius: 2 },
      splitLine: { show: false }
    },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data }]
  }
})

function calcRate(row) {
  if (!kpi.value?.targetValue || !row.actualValue) return 0
  return Math.min(Math.round(row.actualValue / kpi.value.targetValue * 100), 100)
}

function achievementClass(row) {
  const r = calcRate(row)
  if (r >= 80) return 'text-emerald-600 font-bold'
  if (r >= 50) return 'text-amber-600 font-bold'
  return 'text-red-500 font-bold'
}

const frequencyLabel = (f) => ({ DAILY: '매일', WEEKLY: '매주', MONTHLY: '매월' }[f] ?? f)
const kpiTypeLabel = (t) => ({ NUMERIC: '수치', PERCENTAGE: '퍼센트', BOOLEAN: '달성여부' }[t] ?? t)
const statusLabel = (s) => ({ ACTIVE: '활성', PAUSED: '일시정지', COMPLETED: '완료' }[s] ?? s)
const statusTagType = (s) => ({ ACTIVE: 'success', PAUSED: 'warning', COMPLETED: 'info' }[s] ?? '')

// ── CAT7: 마일스톤 (localStorage) ──
const showMilestoneForm = ref(false)
const newMs = reactive({ label: '', target: null, dueDate: null })
const milestones = ref([])

function msKey() { return `workoop-ms-${route.params.id}` }

function loadMilestones() {
  try { milestones.value = JSON.parse(localStorage.getItem(msKey()) || '[]') } catch { milestones.value = [] }
}

function saveMilestones() { localStorage.setItem(msKey(), JSON.stringify(milestones.value)) }

function addMilestone() {
  if (!newMs.label.trim()) return
  milestones.value.push({ id: Date.now(), label: newMs.label, target: newMs.target, dueDate: newMs.dueDate, done: false })
  saveMilestones()
  Object.assign(newMs, { label: '', target: null, dueDate: null })
  showMilestoneForm.value = false
}

function toggleMilestone(ms) {
  ms.done = !ms.done
  saveMilestones()
}

function deleteMilestone(id) {
  milestones.value = milestones.value.filter(m => m.id !== id)
  saveMilestones()
}

onMounted(() => { loadKpi(); loadMilestones() })
</script>

<style scoped>
.page-container { padding: 24px 28px; }

.detail-breadcrumb {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 14px; font-size: 13px;
}
.bc-link    { color: var(--color-project); text-decoration: none; font-weight: 500; }
.bc-link:hover { text-decoration: underline; }
.bc-sep     { color: var(--border-color); }
.bc-current { color: var(--text-secondary); font-weight: 500; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.detail-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 18px; background: var(--bg-card);
  border-radius: var(--radius-lg); padding: 18px 20px;
  border: 1px solid var(--border-color); box-shadow: var(--shadow-xs);
}
.header-left { display: flex; align-items: flex-start; gap: 14px; }

.back-btn {
  width: 34px; height: 34px; border: 1px solid var(--border-color);
  border-radius: var(--radius-md); background: var(--bg-card);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: all var(--transition-fast); flex-shrink: 0; margin-top: 2px;
}
.back-btn:hover { background: var(--bg-hover); color: var(--text-primary); }

.detail-kpi-name { font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 7px; letter-spacing: -0.02em; }
.detail-kpi-meta { display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }

.meta-cat  { font-size: 11px; font-weight: 600; padding: 3px 9px; border-radius: var(--radius-sm); }
.meta-chip { font-size: 11px; color: var(--text-secondary); background: var(--bg-hover); padding: 3px 9px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); }

.info-cards { display: flex; gap: 10px; margin-bottom: 18px; flex-wrap: wrap; }
.info-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 12px 18px; min-width: 110px;
  box-shadow: var(--shadow-xs);
}
.info-label { font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
.info-value { font-size: 18px; font-weight: 700; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }

.period-selector { display: flex; gap: 7px; margin-bottom: 14px; }

.chart-section {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 18px 20px; margin-bottom: 16px;
  box-shadow: var(--shadow-xs);
}
.chart-title { font-size: 14px; font-weight: 600; color: var(--text-primary); margin-bottom: 12px; letter-spacing: -0.01em; }

.bool-summary-row { display: flex; gap: 10px; margin-bottom: 4px; }
.bool-stat { flex: 1; border-radius: var(--radius-md); padding: 12px 16px; text-align: center; }
.bool-stat.achieved     { background: rgba(16,185,129,0.08); border: 1px solid rgba(16,185,129,0.25); }
.bool-stat.not-achieved { background: rgba(239,68,68,0.08);  border: 1px solid rgba(239,68,68,0.2); }
.bool-stat.rate         { background: rgba(48,127,226,0.08); border: 1px solid rgba(48,127,226,0.25); }

.bool-stat-value { font-size: 26px; font-weight: 800; line-height: 1; margin-bottom: 4px; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.bool-stat.achieved .bool-stat-value     { color: #059669; }
.bool-stat.not-achieved .bool-stat-value { color: #DC2626; }
.bool-stat.rate .bool-stat-value         { color: var(--color-project); }
.bool-stat-label { font-size: 11px; font-weight: 600; color: var(--text-secondary); }

.no-data-msg { text-align: center; color: var(--text-muted); font-size: 13px; padding: 32px 0; }

.heatmap-section { overflow-x: auto; }
.heatmap-header  { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.heatmap-legend  { display: flex; align-items: center; gap: 4px; }
.legend-cell     { width: 11px; height: 11px; border-radius: 2px; display: inline-block; }
.legend-label    { font-size: 11px; color: var(--text-muted); }

.milestone-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.ms-add-btn {
  font-size: 12px; font-weight: 700; color: var(--color-project);
  background: rgba(48,127,226,0.08); border: 1px solid rgba(48,127,226,0.25); border-radius: var(--radius-md);
  padding: 4px 12px; cursor: pointer; transition: all var(--transition-fast); font-family: inherit;
}
.ms-add-btn:hover { background: rgba(48,127,226,0.14); }

.ms-form {
  display: flex; gap: 8px; align-items: center; flex-wrap: wrap;
  background: var(--bg-hover); border-radius: var(--radius-md); padding: 12px; margin-bottom: 12px;
  border: 1px solid var(--border-color);
}

.ms-list { display: flex; flex-direction: column; gap: 7px; }
.ms-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); box-shadow: var(--shadow-xs); transition: all var(--transition-fast);
}
.ms-item.done { opacity: 0.6; background: rgba(16,185,129,0.05); border-color: rgba(16,185,129,0.3); }
.ms-check { cursor: pointer; font-size: 18px; flex-shrink: 0; }
.ms-info  { flex: 1; }
.ms-label { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.ms-item.done .ms-label { text-decoration: line-through; color: var(--text-muted); }
.ms-meta  { display: flex; gap: 10px; font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.ms-del {
  width: 22px; height: 22px; border: none; border-radius: 50%;
  background: var(--bg-hover); color: var(--text-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all var(--transition-fast);
}
.ms-del:hover { background: rgba(239,68,68,0.1); color: var(--color-danger); }

.records-section {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 18px 20px; box-shadow: var(--shadow-xs);
}
.section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); letter-spacing: -0.01em; }
</style>
