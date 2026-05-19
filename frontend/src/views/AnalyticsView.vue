<template>
  <div class="page-container">

    <!-- 기간 선택 -->
    <div class="analytics-header">
      <div class="period-tabs">
        <button
          v-for="p in periodOptions"
          :key="p.value"
          class="period-tab"
          :class="{ active: selectedPeriod === p.value }"
          @click="changePeriod(p.value)"
        >{{ p.label }}</button>
      </div>
      <div class="period-range-txt">
        {{ rangeLabel }}
      </div>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="loading-center">
      <el-icon class="is-loading" :size="36"><Loading /></el-icon>
    </div>

    <template v-else>
      <!-- ① 개인 성과 점수 (ANL-3) -->
      <div class="score-section">
        <div class="score-card">
          <div class="score-ring-wrap">
            <svg viewBox="0 0 120 120" class="score-ring">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#E2E8F0" stroke-width="10"/>
              <circle
                cx="60" cy="60" r="50" fill="none"
                :stroke="scoreColor"
                stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="`${scoreCircumference * performanceScore / 100} ${scoreCircumference}`"
                transform="rotate(-90 60 60)"
                style="transition: stroke-dasharray 0.8s ease"
              />
            </svg>
            <div class="score-inner">
              <div class="score-num" :style="{ color: scoreColor }">{{ performanceScore }}</div>
              <div class="score-unit">/ 100</div>
            </div>
          </div>
          <div class="score-info">
            <div class="score-title">개인 성과 점수</div>
            <div class="score-desc" :style="{ color: scoreColor }">{{ scoreLabel }}</div>
            <div class="score-sub">{{ rangeLabel }} 기간 기준</div>
            <div class="score-breakdown">
              <div v-for="s in scoreBreakdown" :key="s.label" class="score-item">
                <span class="score-item-label">{{ s.label }}</span>
                <div class="score-item-bar-bg">
                  <div class="score-item-bar" :style="{ width: s.rate + '%', background: s.color }"></div>
                </div>
                <span class="score-item-val">{{ s.rate }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ② KPI 달성률 랭킹 (Top 5 / Bottom 5) -->
      <div class="two-col">
        <div class="analytics-card">
          <div class="ac-title"><Trophy :size="15" /> 달성률 Top KPI</div>
          <div class="rank-list">
            <div v-for="(kpi, i) in topKpis" :key="kpi.id" class="rank-item">
              <div class="rank-num" :class="`rank-${i+1}`">{{ i + 1 }}</div>
              <div class="rank-info">
                <div class="rank-name">{{ kpi.name }}</div>
                <div class="rank-bar-wrap">
                  <div class="rank-bar" :style="{ width: Math.min(kpi.rate, 100) + '%', background: '#10B981' }"></div>
                </div>
              </div>
              <div class="rank-rate good">{{ kpi.rate }}%</div>
            </div>
            <div v-if="topKpis.length === 0" class="no-data-sm">데이터 없음</div>
          </div>
        </div>

        <div class="analytics-card">
          <div class="ac-title"><AlertTriangle :size="15" /> 달성률 Bottom KPI</div>
          <div class="rank-list">
            <div v-for="(kpi, i) in bottomKpis" :key="kpi.id" class="rank-item">
              <div class="rank-num bad">{{ i + 1 }}</div>
              <div class="rank-info">
                <div class="rank-name">{{ kpi.name }}</div>
                <div class="rank-bar-wrap">
                  <div class="rank-bar" :style="{ width: Math.min(kpi.rate, 100) + '%', background: '#EF4444' }"></div>
                </div>
              </div>
              <div class="rank-rate bad">{{ kpi.rate }}%</div>
            </div>
            <div v-if="bottomKpis.length === 0" class="no-data-sm">데이터 없음</div>
          </div>
        </div>
      </div>

      <!-- ③ 카테고리별 달성률 (ANL-1) -->
      <div class="analytics-card" v-if="categoryData.length > 0">
        <div class="ac-title"><BarChart3 :size="15" /> 카테고리별 달성률</div>
        <div class="cat-bars">
          <div v-for="cat in categoryData" :key="cat.name" class="cat-bar-row">
            <div class="cat-bar-label">
              <span class="cat-dot" :style="{ background: cat.color }"></span>
              {{ cat.name }}
            </div>
            <div class="cat-bar-track">
              <div
                class="cat-bar-fill"
                :style="{ width: cat.rate + '%', background: cat.color }"
              ></div>
            </div>
            <div class="cat-bar-val">{{ cat.rate }}%</div>
          </div>
        </div>
      </div>

      <!-- ④ 기간 비교 (ANL-2) -->
      <div class="analytics-card" v-if="comparisonData.current !== null">
        <div class="ac-title"><TrendingUp :size="15" /> 기간 비교</div>
        <div class="comparison-row">
          <div class="comp-item">
            <div class="comp-period">이전 기간</div>
            <div class="comp-val" :style="{ color: '#64748B' }">{{ comparisonData.prev }}%</div>
            <div class="comp-label">평균 달성률</div>
          </div>
          <div class="comp-arrow" :class="comparisonData.diff >= 0 ? 'up' : 'down'">
            {{ comparisonData.diff >= 0 ? '▲' : '▼' }} {{ Math.abs(comparisonData.diff) }}%p
          </div>
          <div class="comp-item">
            <div class="comp-period">이번 기간</div>
            <div class="comp-val" :style="{ color: comparisonData.diff >= 0 ? '#10B981' : '#EF4444' }">
              {{ comparisonData.current }}%
            </div>
            <div class="comp-label">평균 달성률</div>
          </div>
        </div>
      </div>

      <!-- ⑤ 주간 달성률 추이 차트 -->
      <div class="analytics-card" v-if="trendChartOption">
        <div class="ac-title"><LineChartIcon :size="15" /> 달성률 추이</div>
        <v-chart :option="trendChartOption" style="height:220px;width:100%" autoresize />
      </div>

      <!-- ⑥ 기록 빈도 분석 -->
      <div class="two-col">
        <div class="analytics-card">
          <div class="ac-title"><CalendarDays :size="15" /> 기록 현황</div>
          <div class="stat-rows">
            <div class="stat-row">
              <span>총 기록 수</span>
              <strong>{{ totalRecords }}건</strong>
            </div>
            <div class="stat-row">
              <span>기록 있는 날</span>
              <strong>{{ activeDays }}일</strong>
            </div>
            <div class="stat-row">
              <span>평균 일일 기록</span>
              <strong>{{ avgDailyRecords }}건/일</strong>
            </div>
            <div class="stat-row">
              <span>가장 많이 기록한 날</span>
              <strong>{{ mostActiveDay || '-' }}</strong>
            </div>
          </div>
        </div>

        <div class="analytics-card">
          <div class="ac-title"><Target :size="15" /> KPI 상태</div>
          <div class="stat-rows">
            <div class="stat-row">
              <span>활성 KPI</span>
              <strong>{{ store.activeKpis.length }}개</strong>
            </div>
            <div class="stat-row">
              <span>100% 달성 KPI</span>
              <strong class="text-green">{{ perfectKpis }}개</strong>
            </div>
            <div class="stat-row">
              <span>50% 미만 KPI</span>
              <strong class="text-red">{{ lowKpis }}개</strong>
            </div>
            <div class="stat-row">
              <span>기간 내 미기록 KPI</span>
              <strong class="text-amber">{{ noRecordKpis }}개</strong>
            </div>
          </div>
        </div>
      </div>

      <!-- ⑦ 활동 히트맵 (최근 1년) -->
      <div class="analytics-card" v-if="heatmapOption">
        <div class="ac-title"><Activity :size="15" /> 활동 히트맵 (최근 1년)</div>
        <v-chart :option="heatmapOption" style="height:200px;width:100%" autoresize />
      </div>

      <!-- ⑧ + ⑨ 도넛 + 요일별 -->
      <div class="two-col">
        <div class="analytics-card" v-if="statusDonutOption">
          <div class="ac-title"><PieChartIcon :size="15" /> KPI 상태 분포</div>
          <v-chart :option="statusDonutOption" style="height:240px;width:100%" autoresize />
        </div>

        <div class="analytics-card" v-if="weekdayBarOption">
          <div class="ac-title"><CalendarDays :size="15" /> 요일별 기록 분포</div>
          <v-chart :option="weekdayBarOption" style="height:240px;width:100%" autoresize />
        </div>
      </div>

    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useKpiStore } from '@/store/kpiStore'
import { recordApi } from '@/api/kpiApi'
import dayjs from 'dayjs'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart, HeatmapChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, CalendarComponent, VisualMapComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { Trophy, AlertTriangle, BarChart3, TrendingUp, LineChart as LineChartIcon, CalendarDays, Target, PieChart as PieChartIcon, Activity } from 'lucide-vue-next'

use([LineChart, BarChart, PieChart, HeatmapChart, GridComponent, TooltipComponent, CalendarComponent, VisualMapComponent, LegendComponent, CanvasRenderer])

const store = useKpiStore()
const loading = ref(false)
const records = ref([])
const yearRecords = ref([])

const selectedPeriod = ref(30)
const periodOptions = [
  { label: '2주', value: 14 },
  { label: '1개월', value: 30 },
  { label: '3개월', value: 90 },
  { label: '6개월', value: 180 }
]

const scoreCircumference = 2 * Math.PI * 50

const rangeStart = computed(() => dayjs().subtract(selectedPeriod.value, 'day').format('YYYY-MM-DD'))
const rangeEnd   = computed(() => dayjs().format('YYYY-MM-DD'))
const rangeLabel = computed(() => `${rangeStart.value} ~ ${rangeEnd.value}`)

async function loadData() {
  loading.value = true
  try {
    await store.fetchKpis()
    await store.fetchDashboard()
    const res = await recordApi.getByDateRange(rangeStart.value, rangeEnd.value)
    records.value = res.data || []

    const yearStart = dayjs().subtract(364, 'day').format('YYYY-MM-DD')
    const yearEnd   = dayjs().format('YYYY-MM-DD')
    const yres = await recordApi.getByDateRange(yearStart, yearEnd)
    yearRecords.value = yres.data || []
  } finally {
    loading.value = false
  }
}

function changePeriod(val) {
  selectedPeriod.value = val
  loadData()
}

// KPI별 달성률 계산
const kpiRates = computed(() => {
  const kpiMap = {}
  store.activeKpis.forEach(k => { kpiMap[k.id] = { ...k, values: [], rate: 0 } })

  records.value.forEach(r => {
    if (!kpiMap[r.kpiId]) return
    kpiMap[r.kpiId].values.push(r)
  })

  return Object.values(kpiMap).map(k => {
    if (k.values.length === 0) return { ...k, rate: 0 }
    let totalRate = 0
    k.values.forEach(r => {
      if (k.kpiType === 'BOOLEAN') {
        totalRate += r.booleanValue ? 100 : 0
      } else if (k.targetValue && r.actualValue != null) {
        totalRate += Math.min(r.actualValue / k.targetValue * 100, 100)
      }
    })
    return { ...k, rate: Math.round(totalRate / k.values.length) }
  })
})

// 성과 점수
const performanceScore = computed(() => {
  if (kpiRates.value.length === 0) return 0
  const total = kpiRates.value.reduce((s, k) => s + k.rate, 0)
  return Math.round(total / kpiRates.value.length)
})

const scoreColor = computed(() => {
  const s = performanceScore.value
  if (s >= 80) return '#10B981'
  if (s >= 60) return '#3B82F6'
  if (s >= 40) return '#F59E0B'
  return '#EF4444'
})

const scoreLabel = computed(() => {
  const s = performanceScore.value
  if (s >= 90) return '탁월한 성과!'
  if (s >= 80) return '우수한 성과'
  if (s >= 60) return '양호한 성과'
  if (s >= 40) return '보통 수준'
  return '개선이 필요합니다'
})

const scoreBreakdown = computed(() => {
  return kpiRates.value.slice(0, 4).map(k => ({
    label: k.name.length > 10 ? k.name.slice(0, 10) + '…' : k.name,
    rate: k.rate,
    color: k.rate >= 80 ? '#10B981' : k.rate >= 50 ? '#F59E0B' : '#EF4444'
  }))
})

// Top/Bottom KPI
const topKpis = computed(() =>
  [...kpiRates.value].sort((a, b) => b.rate - a.rate).slice(0, 5)
)
const bottomKpis = computed(() =>
  [...kpiRates.value].sort((a, b) => a.rate - b.rate).slice(0, 5)
)

// 카테고리별 달성률
const categoryData = computed(() => {
  const catMap = {}
  kpiRates.value.forEach(k => {
    const catName = k.category?.name || '기타'
    const catColor = k.category?.color || '#94A3B8'
    if (!catMap[catName]) catMap[catName] = { name: catName, color: catColor, rates: [] }
    catMap[catName].rates.push(k.rate)
  })
  return Object.values(catMap).map(c => ({
    ...c,
    rate: Math.round(c.rates.reduce((s, r) => s + r, 0) / c.rates.length)
  })).sort((a, b) => b.rate - a.rate)
})

// 기간 비교
const comparisonData = computed(() => {
  const prevStart = dayjs().subtract(selectedPeriod.value * 2, 'day').format('YYYY-MM-DD')
  const prevEnd   = dayjs().subtract(selectedPeriod.value + 1, 'day').format('YYYY-MM-DD')
  // 현재 기간 평균 = performanceScore
  // 이전 기간은 별도 API 호출 필요 → dashboard의 trendPoints 활용
  const points = store.dashboard?.trendPoints || []
  if (points.length < 14) return { current: performanceScore.value, prev: null, diff: null }
  const withData = arr => arr.filter(p => p.recordedCount > 0)
  const avg = arr => {
    const v = withData(arr)
    return v.length === 0 ? 0 : Math.round(v.reduce((s, p) => s + p.avgAchievementRate, 0) / v.length)
  }
  const prev = avg(points.slice(0, 7))
  const curr = avg(points.slice(7))
  return { current: curr, prev, diff: curr - prev }
})

// 추이 차트
const trendChartOption = computed(() => {
  const points = store.dashboard?.trendPoints || []
  if (points.length === 0) return null
  return {
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>달성률: ${p[0].value}%` },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: points.map(p => p.date.slice(5)),
      axisLabel: { fontSize: 10, color: '#94A3B8' }
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { formatter: '{value}%', fontSize: 10, color: '#94A3B8' }
    },
    series: [{
      type: 'line', smooth: true,
      data: points.map(p => Math.round(p.avgAchievementRate)),
      lineStyle: { color: '#3B82F6', width: 2.5 },
      itemStyle: { color: '#3B82F6' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(59,130,246,0.25)' }, { offset: 1, color: 'rgba(59,130,246,0)' }]
        }
      }
    }]
  }
})

// 기록 통계
const totalRecords = computed(() => records.value.length)
const activeDays   = computed(() => new Set(records.value.map(r => r.recordedDate)).size)
const avgDailyRecords = computed(() =>
  activeDays.value === 0 ? 0 : Math.round(totalRecords.value / activeDays.value * 10) / 10
)
const mostActiveDay = computed(() => {
  const dayCounts = {}
  records.value.forEach(r => { dayCounts[r.recordedDate] = (dayCounts[r.recordedDate] || 0) + 1 })
  const entries = Object.entries(dayCounts)
  if (!entries.length) return null
  return entries.sort((a, b) => b[1] - a[1])[0][0]
})

const perfectKpis   = computed(() => kpiRates.value.filter(k => k.rate >= 100).length)
const lowKpis       = computed(() => kpiRates.value.filter(k => k.rate < 50).length)
const noRecordKpis  = computed(() => kpiRates.value.filter(k => k.rate === 0).length)

// ⑦ 활동 히트맵 (최근 1년) — GitHub 잔디 스타일
const heatmapOption = computed(() => {
  if (yearRecords.value.length === 0) return null
  const counts = {}
  yearRecords.value.forEach(r => {
    counts[r.recordedDate] = (counts[r.recordedDate] || 0) + 1
  })
  const data = Object.entries(counts).map(([d, c]) => [d, c])
  const maxCount = Math.max(1, ...Object.values(counts))
  const yearStart = dayjs().subtract(364, 'day').format('YYYY-MM-DD')
  const yearEnd   = dayjs().format('YYYY-MM-DD')
  return {
    tooltip: {
      formatter: p => `${p.value[0]}<br/>기록: ${p.value[1]}건`
    },
    visualMap: {
      min: 0, max: maxCount,
      show: true, orient: 'horizontal', left: 'center', bottom: 0,
      itemWidth: 10, itemHeight: 100,
      textStyle: { fontSize: 10, color: '#94A3B8' },
      inRange: { color: ['#F1F5F9', '#BBF7D0', '#86EFAC', '#22C55E', '#15803D'] }
    },
    calendar: {
      top: 20, left: 36, right: 16, bottom: 36,
      range: [yearStart, yearEnd],
      cellSize: ['auto', 14],
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      splitLine: { show: false },
      dayLabel: { fontSize: 10, color: '#94A3B8' },
      monthLabel: { fontSize: 10, color: '#94A3B8' },
      yearLabel: { show: false }
    },
    series: [{
      type: 'heatmap', coordinateSystem: 'calendar', data
    }]
  }
})

// ⑧ KPI 상태 분포 도넛
const statusDonutOption = computed(() => {
  const rates = kpiRates.value
  if (rates.length === 0) return null
  const buckets = [
    { name: '🔥 100% 달성', min: 100, max: Infinity, color: '#10B981' },
    { name: '✅ 70%+ 우수',  min: 70,  max: 100,     color: '#3B82F6' },
    { name: '🟡 40%+ 보통',  min: 40,  max: 70,      color: '#F59E0B' },
    { name: '🔴 40% 미만',   min: 0.01, max: 40,     color: '#EF4444' },
    { name: '⚪ 미기록',     min: -1,   max: 0.01,   color: '#CBD5E1' }
  ]
  const data = buckets.map(b => ({
    name: b.name,
    value: rates.filter(r => r.rate >= b.min && r.rate < b.max).length,
    itemStyle: { color: b.color }
  })).filter(d => d.value > 0)

  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}개 ({d}%)' },
    legend: {
      orient: 'vertical', left: 0, top: 'middle',
      textStyle: { fontSize: 11, color: '#64748B' },
      itemWidth: 12, itemHeight: 12
    },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['65%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      labelLine: { show: false },
      data
    }]
  }
})

// ⑨ 요일별 기록 분포
const weekdayBarOption = computed(() => {
  if (records.value.length === 0) return null
  const days = ['일', '월', '화', '수', '목', '금', '토']
  const counts = [0, 0, 0, 0, 0, 0, 0]
  records.value.forEach(r => {
    const d = dayjs(r.recordedDate).day()
    counts[d]++
  })
  // 월~일 순서로 재정렬
  const reorder = [1, 2, 3, 4, 5, 6, 0]
  const labels  = reorder.map(i => days[i])
  const values  = reorder.map(i => counts[i])
  const maxV = Math.max(...values)

  return {
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}요일<br/>${p[0].value}건` },
    grid: { left: 36, right: 16, top: 16, bottom: 28 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 11, color: '#64748B' },
      axisLine: { lineStyle: { color: '#E2E8F0' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#94A3B8' },
      splitLine: { lineStyle: { color: '#F1F5F9' } }
    },
    series: [{
      type: 'bar',
      data: values.map(v => ({
        value: v,
        itemStyle: { color: v === maxV && v > 0 ? '#3B82F6' : '#93C5FD' }
      })),
      barWidth: '52%',
      itemStyle: { borderRadius: [6, 6, 0, 0] }
    }]
  }
})

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 24px 28px; display: flex; flex-direction: column; gap: 20px; }

/* 헤더 */
.analytics-header {
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 16px 20px; box-shadow: var(--shadow-xs);
}

.period-tabs { display: flex; gap: 8px; }
.period-tab {
  padding: 6px 16px; border-radius: 99px; border: 1.5px solid var(--border-color);
  background: var(--bg-card); font-size: 0.82rem; font-weight: 600; color: var(--text-secondary);
  cursor: pointer; transition: all var(--transition-fast);
}
.period-tab:hover { border-color: var(--color-project); color: var(--color-project); }
.period-tab.active { background: var(--color-project); border-color: var(--color-project); color: white; }

.period-range-txt { font-size: 0.8rem; color: var(--text-muted); }

.loading-center { display: flex; justify-content: center; align-items: center; height: 300px; }

/* 성과 점수 카드 */
.score-section { }
.score-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-xl); padding: 28px; box-shadow: var(--shadow-xs);
  display: flex; align-items: center; gap: 36px; flex-wrap: wrap;
}

.score-ring-wrap {
  position: relative; width: 140px; height: 140px; flex-shrink: 0;
}
.score-ring { width: 140px; height: 140px; }
.score-inner {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  text-align: center;
}
.score-num { font-size: 2.2rem; font-weight: 900; line-height: 1; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.score-unit { font-size: 0.75rem; color: var(--text-muted); }

.score-info { flex: 1; min-width: 200px; }
.score-title { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; }
.score-desc { font-size: 0.9rem; font-weight: 700; margin-bottom: 4px; }
.score-sub { font-size: 0.78rem; color: var(--text-muted); margin-bottom: 16px; }

.score-breakdown { display: flex; flex-direction: column; gap: 8px; }
.score-item { display: flex; align-items: center; gap: 8px; }
.score-item-label { font-size: 0.75rem; color: var(--text-secondary); width: 100px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.score-item-bar-bg { flex: 1; height: 6px; background: var(--bg-hover); border-radius: 99px; }
.score-item-bar { height: 6px; border-radius: 99px; transition: width 0.6s ease; }
.score-item-val { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); width: 36px; text-align: right; font-family: 'JetBrains Mono', monospace; }

/* 카드 공통 */
.analytics-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 22px; box-shadow: var(--shadow-xs);
}
.ac-title { font-size: 0.95rem; font-weight: 700; color: var(--text-primary); margin-bottom: 16px; display: flex; align-items: center; gap: 7px; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
@media (max-width: 700px) { .two-col { grid-template-columns: 1fr; } }

/* 랭킹 */
.rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank-item { display: flex; align-items: center; gap: 10px; }
.rank-num {
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--bg-hover); color: var(--text-secondary);
  font-size: 0.72rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.rank-num.rank-1 { background: rgba(234,179,8,0.15); color: #A16207; }
.rank-num.rank-2 { background: var(--bg-hover); color: var(--text-secondary); }
.rank-num.rank-3 { background: rgba(180,83,9,0.12); color: #B45309; }
.rank-num.bad { background: rgba(239,68,68,0.12); color: var(--color-danger); }

.rank-info { flex: 1; }
.rank-name { font-size: 0.82rem; font-weight: 600; color: var(--text-primary); margin-bottom: 4px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-bar-wrap { height: 5px; background: var(--bg-hover); border-radius: 99px; }
.rank-bar { height: 5px; border-radius: 99px; transition: width 0.6s ease; }
.rank-rate { font-size: 0.78rem; font-weight: 700; width: 38px; text-align: right; font-family: 'JetBrains Mono', monospace; }
.rank-rate.good { color: var(--color-success); }
.rank-rate.bad { color: var(--color-danger); }
.no-data-sm { font-size: 0.82rem; color: var(--text-muted); text-align: center; padding: 16px 0; }

/* 카테고리 바 */
.cat-bars { display: flex; flex-direction: column; gap: 12px; }
.cat-bar-row { display: flex; align-items: center; gap: 10px; }
.cat-bar-label { display: flex; align-items: center; gap: 6px; font-size: 0.82rem; font-weight: 600;
  color: var(--text-primary); width: 110px; }
.cat-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.cat-bar-track { flex: 1; height: 10px; background: var(--bg-hover); border-radius: 99px; }
.cat-bar-fill { height: 10px; border-radius: 99px; transition: width 0.6s ease; }
.cat-bar-val { font-size: 0.82rem; font-weight: 700; color: var(--text-secondary); width: 40px; text-align: right; font-family: 'JetBrains Mono', monospace; }

/* 기간 비교 */
.comparison-row { display: flex; align-items: center; justify-content: center; gap: 32px; }
.comp-item { text-align: center; }
.comp-period { font-size: 0.75rem; color: var(--text-muted); margin-bottom: 4px; }
.comp-val { font-size: 2rem; font-weight: 900; line-height: 1; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.comp-label { font-size: 0.75rem; color: var(--text-muted); margin-top: 4px; }
.comp-arrow { font-size: 1.1rem; font-weight: 800; }
.comp-arrow.up { color: var(--color-success); }
.comp-arrow.down { color: var(--color-danger); }

/* 통계 행 */
.stat-rows { display: flex; flex-direction: column; gap: 12px; }
.stat-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 0.85rem; color: var(--text-secondary);
  padding-bottom: 10px; border-bottom: 1px solid var(--border-color);
}
.stat-row:last-child { border-bottom: none; padding-bottom: 0; }
.stat-row strong { color: var(--text-primary); font-weight: 700; }
.text-green { color: var(--color-success) !important; }
.text-red { color: var(--color-danger) !important; }
.text-amber { color: var(--color-warning) !important; }
</style>
