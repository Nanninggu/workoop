<template>
  <div class="page-container">

    <!-- 헤더 -->
    <div class="td-header">
      <div class="td-header-left">
        <h1 class="td-title">팀 성과 대시보드</h1>
        <span class="td-org">{{ orgStore.currentOrg?.name }}</span>
      </div>
      <div class="td-header-right">
        <div class="period-tabs">
          <button
            v-for="p in periods"
            :key="p.value"
            class="period-tab"
            :class="{ active: period === p.value }"
            @click="period = p.value; load()"
          >{{ p.label }}</button>
        </div>
        <button class="btn-refresh" @click="load()" :disabled="loading">
          <RefreshCw :size="15" :class="{ 'spin': loading }" />
        </button>
      </div>
    </div>

    <div v-if="loading && !data" class="td-loading">
      <div v-for="i in 6" :key="i" class="sk-widget"></div>
    </div>

    <template v-else-if="data">

      <!-- Row 1: KPI 스코어보드 + MVP + 번아웃 -->
      <div class="td-row">

        <!-- 팀 KPI 스코어보드 -->
        <div class="td-card kpi-card">
          <div class="card-title">
            <Target :size="15" /> 팀 KPI 스코어보드
          </div>
          <div v-if="data.teamKpis.length === 0" class="empty-hint">
            팀 KPI가 없습니다. KPI 관리에서 scope를 TEAM으로 설정하세요.
          </div>
          <div v-for="kpi in data.teamKpis" :key="kpi.kpiId" class="kpi-row">
            <div class="kpi-row-top">
              <span class="kpi-name">{{ kpi.name }}</span>
              <span class="kpi-rate" :style="{ color: rateColor(kpi.achievementRate) }">
                {{ kpi.achievementRate }}%
              </span>
            </div>
            <div class="kpi-bar-wrap">
              <div
                class="kpi-bar-fill"
                :style="{ width: kpi.achievementRate + '%', background: rateColor(kpi.achievementRate) }"
              ></div>
            </div>
            <div class="kpi-row-bottom">
              <span class="kpi-current">현재 {{ fmtNum(kpi.currentValue) }}{{ kpi.unit || '' }}</span>
              <span class="kpi-target">목표 {{ fmtNum(kpi.targetValue) }}{{ kpi.unit || '' }}</span>
            </div>
          </div>
        </div>

        <!-- MVP 카드 -->
        <div class="td-card mvp-card">
          <div class="card-title"><Trophy :size="15" /> 이번 기간 MVP</div>
          <div v-if="data.mvp" class="mvp-body">
            <div class="mvp-avatar">{{ data.mvp.userName.charAt(0).toUpperCase() }}</div>
            <div class="mvp-name">{{ data.mvp.userName }}</div>
            <div class="mvp-stats">
              <div class="mvp-stat">
                <span class="mvp-stat-val">{{ data.mvp.doneTasks }}</span>
                <span class="mvp-stat-label">완료 태스크</span>
              </div>
              <div class="mvp-divider"></div>
              <div class="mvp-stat">
                <span class="mvp-stat-val">{{ fmtNum(data.mvp.kpiContribution) }}</span>
                <span class="mvp-stat-label">KPI 기여</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-hint">완료된 태스크가 없습니다.</div>

          <!-- 번아웃 경고 -->
          <div v-if="data.burnoutRisks.length > 0" class="burnout-section">
            <div class="burnout-title"><TriangleAlert :size="13" /> 번아웃 위험</div>
            <div
              v-for="m in data.burnoutRisks"
              :key="m.userId"
              class="burnout-row"
              :class="'risk-' + m.riskLevel.toLowerCase()"
            >
              <div class="burnout-dot"></div>
              <span class="burnout-name">{{ m.userName }}</span>
              <span class="burnout-badge">{{ m.riskLevel === 'RISK' ? '위험' : '주의' }}</span>
              <span class="burnout-signals">{{ m.signals.join(' · ') }}</span>
            </div>
          </div>
          <div v-else class="burnout-safe">
            <Heart :size="13" /> 팀 전체 번아웃 위험 없음
          </div>
        </div>

      </div>

      <!-- Row 2: 팀원 기여도 차트 + 속도 차트 -->
      <div class="td-row">

        <!-- 팀원 기여도 -->
        <div class="td-card chart-card">
          <div class="card-title"><Users :size="15" /> 팀원 기여도</div>
          <v-chart v-if="contribOption" :option="contribOption" style="height:220px" autoresize />
          <div v-else class="empty-hint">데이터가 없습니다.</div>
        </div>

        <!-- 업무 완료 속도 -->
        <div class="td-card chart-card">
          <div class="card-title"><TrendingUp :size="15" /> 업무 완료 속도</div>
          <v-chart v-if="velocityOption" :option="velocityOption" style="height:220px" autoresize />
          <div v-else class="empty-hint">데이터가 없습니다.</div>
        </div>

      </div>

      <!-- Row 3: 에너지 히트맵 + 블로커 현황 -->
      <div class="td-row">

        <!-- 에너지 히트맵 -->
        <div class="td-card heatmap-card">
          <div class="card-title"><Zap :size="15" /> 팀 에너지 히트맵</div>
          <div v-if="data.energyMap.length === 0" class="empty-hint">
            팀원들의 스크럼 에너지 데이터가 없습니다.
          </div>
          <div v-else class="heatmap-wrap">
            <div v-for="row in data.energyMap" :key="row.userId" class="heatmap-row">
              <div class="heatmap-label">{{ row.userName }}</div>
              <div class="heatmap-cells">
                <div
                  v-for="(date, idx) in heatmapDates"
                  :key="date"
                  class="heatmap-cell"
                  :style="{ background: energyCellColor(row, date) }"
                  :title="date + ' : 에너지 ' + (energyValue(row, date) || '-')"
                ></div>
              </div>
            </div>
            <div class="heatmap-dates">
              <div class="heatmap-label"></div>
              <div class="heatmap-cells date-labels">
                <span
                  v-for="(date, idx) in heatmapDates"
                  :key="date"
                  class="heatmap-date-label"
                  v-show="idx % 3 === 0"
                >{{ fmtDate(date) }}</span>
              </div>
            </div>
            <div class="heatmap-legend">
              <span class="legend-label">낮음</span>
              <div v-for="i in 5" :key="i" class="legend-cell" :style="{ background: energyColorByVal(i) }"></div>
              <span class="legend-label">높음</span>
            </div>
          </div>
        </div>

        <!-- 블로커 현황 -->
        <div class="td-card blocker-card">
          <div class="card-title">
            <AlertCircle :size="15" /> 오늘의 블로커
            <span v-if="data.blockers.length > 0" class="blocker-count-badge">{{ data.blockers.length }}</span>
          </div>
          <div v-if="data.blockers.length === 0" class="blocker-empty">
            <div class="blocker-ok-icon">✓</div>
            오늘 등록된 블로커가 없습니다
          </div>
          <div v-else class="blocker-list">
            <div
              v-for="b in data.blockers"
              :key="b.userId"
              class="blocker-item"
              :class="'sev-' + (b.severity || 'LOW').toLowerCase()"
            >
              <div class="blocker-item-top">
                <div class="blocker-avatar">{{ b.userName.charAt(0).toUpperCase() }}</div>
                <span class="blocker-user">{{ b.userName }}</span>
                <span class="blocker-sev-badge">{{ b.severity }}</span>
              </div>
              <div class="blocker-text">{{ b.blocker }}</div>
            </div>
          </div>
        </div>

      </div>

    </template>

    <div v-else-if="!loading" class="td-empty">
      조직을 선택하고 팀원을 초대한 후 데이터를 확인하세요.
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useOrgStore } from '@/store/orgStore'
import {
  Target, Trophy, TriangleAlert, Heart, Users, TrendingUp,
  Zap, AlertCircle, RefreshCw
} from 'lucide-vue-next'
import dayjs from 'dayjs'
import api from '@/api/axios'

import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, DataZoomComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, CanvasRenderer])

const orgStore = useOrgStore()

const period  = ref('week')
const loading = ref(false)
const data    = ref(null)

const periods = [
  { value: 'week',    label: '이번 주' },
  { value: 'month',   label: '30일' },
  { value: 'quarter', label: '3개월' },
]

async function load() {
  const orgId = orgStore.currentOrg?.id
  if (!orgId) return
  loading.value = true
  try {
    const res = await api.get('/team-dashboard', { params: { orgId, period: period.value } })
    data.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ── ECharts 옵션 ──────────────────────────────────────────────────────────────

const contribOption = computed(() => {
  if (!data.value?.memberContributions?.length) return null
  const members = data.value.memberContributions
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {
      data: ['완료 태스크', 'KPI 기여'],
      textStyle: { color: '#94A3B8', fontSize: 11 },
      bottom: 0,
    },
    grid: { left: 16, right: 16, top: 10, bottom: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: members.map(m => m.userName),
      axisLabel: { color: '#94A3B8', fontSize: 11 },
      axisLine: { lineStyle: { color: '#334155' } },
    },
    yAxis: [
      { type: 'value', name: '태스크', axisLabel: { color: '#64748B', fontSize: 10 }, splitLine: { lineStyle: { color: '#1E293B' } } },
      { type: 'value', name: 'KPI',   axisLabel: { color: '#64748B', fontSize: 10 }, splitLine: { show: false } },
    ],
    series: [
      {
        name: '완료 태스크',
        type: 'bar',
        yAxisIndex: 0,
        data: members.map(m => m.doneTasks),
        itemStyle: { color: '#6366F1', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 32,
      },
      {
        name: 'KPI 기여',
        type: 'bar',
        yAxisIndex: 1,
        data: members.map(m => Math.round(m.kpiContribution * 10) / 10),
        itemStyle: { color: '#10B981', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 32,
      },
    ],
  }
})

const velocityOption = computed(() => {
  if (!data.value?.velocity?.length) return null
  const pts = data.value.velocity
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      formatter: params => `${params[0].axisValue}<br/>완료 ${params[0].value}개`,
    },
    grid: { left: 16, right: 16, top: 10, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: pts.map(p => dayjs(p.date).format('MM/DD')),
      axisLabel: { color: '#94A3B8', fontSize: 10 },
      axisLine: { lineStyle: { color: '#334155' } },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#64748B', fontSize: 10 },
      splitLine: { lineStyle: { color: '#1E293B' } },
    },
    series: [{
      type: 'line',
      data: pts.map(p => p.doneCount),
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#6366F1', width: 2 },
      itemStyle: { color: '#6366F1' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(99,102,241,0.3)' },
            { offset: 1, color: 'rgba(99,102,241,0.02)' },
          ],
        },
      },
    }],
  }
})

// ── 히트맵 헬퍼 ──────────────────────────────────────────────────────────────

const heatmapDates = computed(() => {
  const days = period.value === 'week' ? 7 : period.value === 'month' ? 30 : 90
  const today = dayjs()
  return Array.from({ length: days }, (_, i) =>
    today.subtract(days - 1 - i, 'day').format('YYYY-MM-DD')
  )
})

function energyValue(row, date) {
  const idx = row.dates.indexOf(date)
  return idx >= 0 ? row.values[idx] : null
}

function energyCellColor(row, date) {
  const v = energyValue(row, date)
  return energyColorByVal(v)
}

function energyColorByVal(v) {
  if (v == null || v === 0) return '#1E293B'
  const colors = ['#1E293B', '#1E3A5F', '#1D4ED8', '#3B82F6', '#60A5FA', '#BAE6FD']
  return colors[Math.min(v, 5)]
}

function fmtDate(d) { return dayjs(d).format('M/D') }
function fmtNum(v)  { return v % 1 === 0 ? v : Math.round(v * 10) / 10 }

function rateColor(rate) {
  if (rate >= 80) return '#10B981'
  if (rate >= 50) return '#F59E0B'
  return '#EF4444'
}
</script>

<style scoped>
.page-container { padding: 24px 28px; max-width: 1300px; margin: 0 auto; }

/* 헤더 */
.td-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px; flex-wrap: wrap; gap: 12px;
}
.td-header-left { display: flex; align-items: center; gap: 12px; }
.td-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0; }
.td-org   { font-size: 13px; color: var(--text-muted); background: var(--bg-hover); padding: 3px 10px; border-radius: 20px; }
.td-header-right { display: flex; align-items: center; gap: 8px; }
.period-tabs { display: flex; background: var(--bg-hover); border-radius: var(--radius-md); padding: 3px; gap: 2px; }
.period-tab {
  padding: 5px 14px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 500;
  color: var(--text-secondary); background: none; border: none; cursor: pointer; transition: all var(--transition-fast);
}
.period-tab.active { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-xs); }
.period-tab:hover:not(.active) { color: var(--text-primary); }
.btn-refresh {
  width: 32px; height: 32px; border-radius: var(--radius-md); background: var(--bg-hover); border: 1px solid var(--border-color);
  cursor: pointer; color: var(--text-secondary); display: flex; align-items: center; justify-content: center;
}
.btn-refresh:hover { color: var(--text-primary); }
.btn-refresh:disabled { opacity: .5; cursor: not-allowed; }
@keyframes spin { to { transform: rotate(360deg); } }
.spin { animation: spin .8s linear infinite; }

/* 그리드 행 */
.td-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }

/* 카드 공통 */
.td-card {
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 20px;
  min-height: 200px; box-shadow: var(--shadow-xs);
}
.card-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 600; color: var(--text-muted);
  margin-bottom: 16px; text-transform: uppercase; letter-spacing: .5px;
}
.empty-hint { font-size: 12px; color: var(--text-muted); text-align: center; padding: 24px 0; }

/* KPI 스코어보드 */
.kpi-row { margin-bottom: 14px; }
.kpi-row-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.kpi-name  { font-size: 13px; color: var(--text-primary); font-weight: 500; }
.kpi-rate  { font-size: 13px; font-weight: 700; font-family: 'JetBrains Mono', monospace; }
.kpi-bar-wrap { height: 6px; background: var(--bg-hover); border-radius: 99px; overflow: hidden; margin-bottom: 3px; }
.kpi-bar-fill { height: 100%; border-radius: 99px; transition: width .5s; }
.kpi-row-bottom { display: flex; justify-content: space-between; }
.kpi-current, .kpi-target { font-size: 10px; color: var(--text-muted); }

/* MVP 카드 */
.mvp-card { display: flex; flex-direction: column; gap: 0; }
.mvp-body { display: flex; flex-direction: column; align-items: center; padding: 8px 0 16px; }
.mvp-avatar {
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  font-size: 22px; font-weight: 700; color: white;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 8px;
}
.mvp-name { font-size: 15px; font-weight: 700; color: var(--text-primary); margin-bottom: 12px; }
.mvp-stats { display: flex; align-items: center; gap: 16px; }
.mvp-stat  { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.mvp-stat-val   { font-size: 20px; font-weight: 800; color: #6366F1; font-family: 'JetBrains Mono', monospace; }
.mvp-stat-label { font-size: 10px; color: var(--text-secondary); }
.mvp-divider { width: 1px; height: 32px; background: var(--border-color); }

/* 번아웃 */
.burnout-section { border-top: 1px solid var(--border-color); padding-top: 12px; margin-top: 4px; }
.burnout-title {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px; font-weight: 600; color: var(--color-warning); margin-bottom: 8px;
}
.burnout-row {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: var(--radius-sm); margin-bottom: 4px;
}
.burnout-row.risk-risk    { background: rgba(239,68,68,.08); }
.burnout-row.risk-caution { background: rgba(245,158,11,.08); }
.burnout-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.risk-risk    .burnout-dot { background: var(--color-danger); }
.risk-caution .burnout-dot { background: var(--color-warning); }
.burnout-name  { font-size: 12px; color: var(--text-primary); font-weight: 500; }
.burnout-badge {
  font-size: 10px; padding: 1px 6px; border-radius: 99px; font-weight: 600; margin-left: auto;
}
.risk-risk    .burnout-badge { background: rgba(239,68,68,.15);  color: var(--color-danger); }
.risk-caution .burnout-badge { background: rgba(245,158,11,.15); color: var(--color-warning); }
.burnout-signals { font-size: 10px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px; }
.burnout-safe { font-size: 12px; color: var(--color-success); display: flex; align-items: center; gap: 5px; padding-top: 8px; border-top: 1px solid var(--border-color); margin-top: 4px; }

/* 차트 카드 */
.chart-card { overflow: hidden; }

/* 히트맵 */
.heatmap-card { grid-column: 1 / 2; }
.heatmap-wrap { display: flex; flex-direction: column; gap: 4px; }
.heatmap-row   { display: flex; align-items: center; gap: 6px; }
.heatmap-label { width: 64px; font-size: 11px; color: var(--text-secondary); text-align: right; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.heatmap-cells { display: flex; gap: 3px; flex: 1; }
.heatmap-cell  { flex: 1; height: 16px; border-radius: 3px; cursor: default; transition: transform .1s; }
.heatmap-cell:hover { transform: scale(1.3); }
.heatmap-dates .heatmap-cells { align-items: center; }
.heatmap-date-label { flex: 1; font-size: 9px; color: var(--text-muted); text-align: left; }
.heatmap-legend { display: flex; align-items: center; gap: 4px; margin-top: 8px; padding-left: 70px; }
.legend-label { font-size: 10px; color: var(--text-muted); }
.legend-cell  { width: 14px; height: 14px; border-radius: 3px; }

/* 블로커 */
.blocker-card { grid-column: 2 / 3; }
.blocker-count-badge {
  background: var(--color-danger); color: white; border-radius: 99px;
  font-size: 10px; padding: 1px 6px; font-weight: 700; margin-left: 4px;
}
.blocker-empty { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 24px 0; color: var(--text-muted); font-size: 12px; }
.blocker-ok-icon { font-size: 24px; color: var(--color-success); }
.blocker-list { display: flex; flex-direction: column; gap: 10px; }
.blocker-item {
  border-radius: var(--radius-md); padding: 10px 12px;
  border-left: 3px solid var(--border-color);
}
.blocker-item.sev-high { border-left-color: var(--color-danger); background: rgba(239,68,68,.05); }
.blocker-item.sev-med  { border-left-color: var(--color-warning); background: rgba(245,158,11,.05); }
.blocker-item.sev-low  { border-left-color: var(--color-project); background: rgba(48,127,226,.05); }
.blocker-item-top { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.blocker-avatar {
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--bg-hover); font-size: 10px; font-weight: 700; color: var(--text-secondary);
  display: flex; align-items: center; justify-content: center;
}
.blocker-user { font-size: 12px; color: var(--text-primary); font-weight: 600; }
.blocker-sev-badge {
  margin-left: auto; font-size: 10px; font-weight: 600; padding: 1px 6px; border-radius: var(--radius-xs);
}
.sev-high .blocker-sev-badge { background: rgba(239,68,68,.15);  color: var(--color-danger); }
.sev-med  .blocker-sev-badge { background: rgba(245,158,11,.15); color: var(--color-warning); }
.sev-low  .blocker-sev-badge { background: rgba(48,127,226,.15); color: var(--color-project); }
.blocker-text { font-size: 12px; color: var(--text-secondary); line-height: 1.4; }

/* 스켈레톤 */
.td-loading { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.sk-widget  { height: 220px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg); }

.td-empty { text-align: center; color: var(--text-muted); padding: 60px; font-size: 14px; }

@media (max-width: 900px) {
  .td-row { grid-template-columns: 1fr; }
  .heatmap-card, .blocker-card { grid-column: 1; }
}
</style>
