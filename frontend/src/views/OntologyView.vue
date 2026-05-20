<template>
  <div class="onto-page">

    <!-- 헤더 -->
    <div class="onto-header">
      <div class="onto-title-wrap">
        <Share2 :size="20" class="onto-icon" />
        <div>
          <div class="onto-title">지식 그래프</div>
          <div class="onto-sub">
            {{ stats.totalTriples?.toLocaleString() }}개 트리플 ·
            {{ stats.countUser }}명 · {{ stats.countTask }}개 태스크 · {{ stats.countKPI }}개 KPI
          </div>
        </div>
      </div>
      <div class="onto-controls">
        <!-- 심화 분석 실행 버튼 -->
        <button class="enrich-btn" @click="runEnrich" :disabled="enriching" title="역량추론·클러스터링·협업 분석 실행">
          <Sparkles :size="14" :class="{ spinning: enriching }" />
          <span>{{ enriching ? '분석 중…' : '심화 분석' }}</span>
        </button>
        <!-- 재동기화 -->
        <button class="sync-btn" @click="runSync" :disabled="syncing" title="H2 → TDB2 재동기화">
          <RefreshCw :size="14" :class="{ spinning: syncing }" />
        </button>
      </div>
    </div>

    <!-- 탭 -->
    <div class="onto-tabs">
      <button v-for="t in tabs" :key="t.key"
        :class="['tab', { active: activeTab === t.key }]"
        @click="switchTab(t.key)">
        <component :is="t.icon" :size="13" />
        {{ t.label }}
      </button>
    </div>

    <!-- 서브 필터 (기본 탭일 때만) -->
    <div v-if="activeTab === 'base'" class="onto-legend">
      <button v-for="f in baseFilters" :key="f.key"
        :class="['legend-filter', { active: baseFilter === f.key }]"
        @click="setBaseFilter(f.key)">{{ f.label }}</button>
      <span class="legend-sep">|</span>
      <span v-for="cat in categories" :key="cat.name" class="legend-item">
        <span class="legend-dot" :style="{ background: catColor(cat.name) }"></span>
        {{ catLabel(cat.name) }}
      </span>
    </div>
    <div v-else-if="activeTab === 'clusters'" class="onto-legend">
      <span v-for="cat in categories" :key="cat.name" class="legend-item">
        <span class="legend-dot" :style="{ background: clusterColor(cat.name) }"></span>
        {{ cat.name }}
      </span>
    </div>
    <div v-else class="onto-legend">
      <span class="legend-hint">
        <component :is="tabIcon" :size="12" />
        {{ tabHint }}
      </span>
    </div>

    <!-- 그래프 영역 -->
    <div class="graph-wrap">
      <div v-if="loading" class="graph-loading">
        <RefreshCw :size="32" class="spinning" />
        <p>{{ loadingMsg }}</p>
      </div>
      <div v-else-if="!nodes.length" class="graph-empty">
        <Sparkles :size="40" class="empty-icon" />
        <p>데이터가 없습니다.</p>
        <p class="empty-hint">상단 "심화 분석" 버튼을 눌러 분석을 실행하세요.</p>
      </div>
      <v-chart v-else class="graph-chart" :option="chartOption" autoresize @click="onNodeClick" />
    </div>

    <!-- 상세 패널 -->
    <Transition name="panel">
      <div v-if="selected" class="detail-panel">
        <div class="dp-header">
          <span class="dp-badge" :style="{ background: catColor(selected.category) }">
            {{ catLabel(selected.category) }}
          </span>
          <button class="dp-close" @click="selected = null"><X :size="16"/></button>
        </div>
        <div class="dp-name">{{ selected.fullName }}</div>
        <div class="dp-props">
          <div v-for="p in selectedProps" :key="p.key" class="dp-row">
            <span class="dp-key">{{ p.key }}</span>
            <span class="dp-val">{{ p.val }}</span>
          </div>
          <div v-if="!selectedProps.length" class="dp-empty">속성 조회 중…</div>
        </div>
      </div>
    </Transition>

    <!-- 심화분석 완료 토스트 -->
    <Transition name="toast">
      <div v-if="enrichToast" class="enrich-toast">
        <Sparkles :size="13" /> {{ enrichToast }}
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, shallowRef } from 'vue'
import { Share2, RefreshCw, X, Sparkles, Network, Tag, Users } from 'lucide-vue-next'
import { use } from 'echarts/core'
import { GraphChart, ScatterChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import api from '@/api/axios'

use([GraphChart, ScatterChart, TooltipComponent, LegendComponent, CanvasRenderer])

// ── 탭 정의 ────────────────────────────────────────────────
const tabs = [
  { key: 'base',          label: '전체 관계',    icon: Network },
  { key: 'skills',        label: '역량 맵',       icon: Sparkles },
  { key: 'clusters',      label: '태스크 클러스터', icon: Tag },
  { key: 'collaboration', label: '협업 네트워크',  icon: Users },
]
const activeTab  = ref('base')
const baseFilter = ref('all')
const baseFilters = [
  { key: 'all',  label: '전체' },
  { key: 'org',  label: '조직·멤버' },
  { key: 'task', label: '태스크·담당자' },
  { key: 'kpi',  label: 'KPI·담당자' },
]

const tabHint = computed(() => ({
  skills:        '노드 클릭 시 해당 사용자의 역량 스킬 확인',
  collaboration: '엣지 두께 = 협업 강도 (공유 프로젝트·태스크 수)',
})[activeTab.value] || '')

const tabIcon = computed(() =>
  tabs.find(t => t.key === activeTab.value)?.icon || Network)

// ── 상태 ───────────────────────────────────────────────────
const loading   = ref(false)
const syncing   = ref(false)
const enriching = ref(false)
const enrichToast = ref('')
const nodes     = ref([])
const edges     = ref([])
const categories = ref([])
const stats     = ref({})
const selected  = ref(null)
const selectedProps = ref([])
const loadingMsg = ref('로딩 중…')

let toastTimer = null
function showToast(msg) {
  enrichToast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { enrichToast.value = '' }, 4000)
}

// ── 색상 ───────────────────────────────────────────────────
const BASE_COLORS = {
  User: '#4F9CF9', Organization: '#F59E0B',
  Project: '#10B981', Task: '#8B5CF6',
  KPI: '#EF4444', Skill: '#F472B6',
}
const CLUSTER_COLORS = {
  '개발': '#6366F1', '디자인': '#EC4899', '테스트/QA': '#F59E0B',
  '커뮤니케이션': '#10B981', '문서화': '#64748B', '기획/분석': '#3B82F6',
  '인프라/DevOps': '#EF4444', '성과관리': '#8B5CF6', '기타': '#94A3B8',
}
function catColor(name)     { return BASE_COLORS[name]    || '#94A3B8' }
function clusterColor(name) { return CLUSTER_COLORS[name] || '#94A3B8' }
function catLabel(name) {
  return { User:'사용자', Organization:'조직', Project:'프로젝트',
           Task:'태스크', KPI:'KPI', Skill:'스킬' }[name] || name
}

// ── ECharts 옵션 ───────────────────────────────────────────
const chartOption = computed(() => {
  const isCollab = activeTab.value === 'collaboration'
  const isCluster = activeTab.value === 'clusters'

  return {
    backgroundColor: '#0f1521',
    tooltip: {
      formatter: p => p.dataType === 'node'
        ? `<b>${p.data.fullName}</b><br/><small>${catLabel(p.data.category)}</small>`
        : `<small>${p.data.label || ''}</small>`
    },
    legend: [{
      data: categories.value.map(c => c.name),
      formatter: n => catLabel(n) !== n ? catLabel(n) : n,
      textStyle: { color: '#A0AEC0', fontSize: 10 },
      bottom: 8, itemWidth: 12, itemHeight: 12,
    }],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      force: {
        repulsion: isCollab ? 400 : isCluster ? 200 : 280,
        gravity: isCollab ? 0.08 : 0.04,
        edgeLength: isCollab ? [120, 300] : [80, 200],
        layoutAnimation: true,
      },
      categories: categories.value.map(c => ({
        name: c.name,
        itemStyle: { color: isCluster ? clusterColor(c.name) : catColor(c.name) },
      })),
      data: nodes.value.map(n => ({
        ...n,
        itemStyle: { color: isCluster ? clusterColor(n.category) : catColor(n.category) },
        symbol: n.symbol || (n.category === 'Skill' ? 'diamond' : 'circle'),
        label: {
          show: true, color: '#E2E8F0',
          fontSize: isCollab ? 11 : 9,
          position: 'bottom',
        },
      })),
      edges: edges.value.map(e => ({
        source: e.source,
        target: e.target,
        lineStyle: {
          color: isCollab ? '#4F9CF9' : '#2d3748',
          width: isCollab ? (e.lineWidth || 1) : 1,
          curveness: 0.1,
          opacity: isCollab ? 0.6 : 0.8,
        },
        label: { show: false, formatter: e.label, fontSize: 9, color: '#64748B' },
      })),
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3, color: '#4F9CF9' },
      },
      edgeSymbol: ['none', activeTab.value === 'collaboration' ? 'none' : 'arrow'],
      edgeSymbolSize: 5,
    }]
  }
})

// ── 데이터 로드 ─────────────────────────────────────────────
async function loadGraph() {
  loading.value = true
  loadingMsg.value = '그래프 로딩 중…'
  try {
    let res
    if (activeTab.value === 'base') {
      res = await api.get('/ontology/graph', { params: { filter: baseFilter.value } })
    } else {
      res = await api.get('/ontology/graph/enriched', { params: { mode: activeTab.value } })
    }
    nodes.value      = res.data?.nodes      || []
    edges.value      = res.data?.edges      || []
    categories.value = res.data?.categories || []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await api.get('/ontology/stats')
  stats.value = res.data || {}
}

async function switchTab(key) {
  activeTab.value = key
  selected.value = null
  await loadGraph()
}

async function setBaseFilter(key) {
  baseFilter.value = key
  await loadGraph()
}

async function runSync() {
  syncing.value = true
  try {
    await api.post('/ontology/sync')
    await Promise.all([loadGraph(), loadStats()])
    showToast('데이터 동기화 완료')
  } finally {
    syncing.value = false
  }
}

async function runEnrich() {
  enriching.value = true
  loadingMsg.value = 'AI 역량 추론 + 클러스터링 + 협업 분석 중…'
  try {
    const res = await api.post('/ontology/enrich')
    const s = res.data || {}
    showToast(`심화 분석 완료 — 스킬 ${s.skills}개 · 클러스터 ${s.tasksClustered}개 · 협업 ${s.collabPairs}쌍`)
    // 심화 분석 탭으로 자동 이동
    if (activeTab.value === 'base') await switchTab('skills')
    else await loadGraph()
  } finally {
    enriching.value = false
  }
}

// ── 노드 클릭 ───────────────────────────────────────────────
async function onNodeClick(params) {
  if (params.dataType !== 'node') return
  selected.value = params.data
  selectedProps.value = []
  try {
    const res = await api.get('/ontology/describe', { params: { uri: params.data.id } })
    selectedProps.value = (res.data || [])
      .filter(r => !r.p?.includes('rdf-syntax') && !r.p?.includes('22-rdf'))
      .map(r => ({
        key: (r.p || '').replace('http://coopwork.io/ontology#', '').replace('has',''),
        val: r.o || ''
      }))
      .filter(r => r.val && r.key)
  } catch {}
}

onMounted(async () => {
  await Promise.all([loadGraph(), loadStats()])
})
</script>

<style scoped>
.onto-page {
  display: flex; flex-direction: column; height: 100%;
  overflow: hidden; background: #0f1521; color: #E2E8F0; position: relative;
}

/* ── 헤더 ── */
.onto-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px 10px; border-bottom: 1px solid #1e293b; flex-shrink: 0;
}
.onto-title-wrap { display: flex; align-items: center; gap: 12px; }
.onto-icon { color: #4F9CF9; }
.onto-title { font-size: 15px; font-weight: 700; color: #F0F4FF; }
.onto-sub { font-size: 11px; color: #64748B; margin-top: 1px; }
.onto-controls { display: flex; align-items: center; gap: 8px; }

.enrich-btn {
  display: flex; align-items: center; gap: 6px; padding: 6px 14px;
  border-radius: 7px; font-size: 12px; font-weight: 600;
  border: 1px solid #8B5CF6; color: #C4B5FD; background: rgba(139,92,246,0.1);
  cursor: pointer; transition: all .15s;
}
.enrich-btn:hover:not(:disabled) { background: rgba(139,92,246,0.2); }
.enrich-btn:disabled { opacity: 0.5; cursor: default; }

.sync-btn {
  width: 30px; height: 30px; border-radius: 7px; border: 1px solid #2d3748;
  background: transparent; color: #718096; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all .15s;
}
.sync-btn:hover:not(:disabled) { border-color: #10B981; color: #10B981; }
.sync-btn:disabled { opacity: 0.5; cursor: default; }

.spinning { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 탭 ── */
.onto-tabs {
  display: flex; gap: 2px; padding: 8px 20px 0;
  border-bottom: 1px solid #1e293b; flex-shrink: 0;
}
.tab {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 14px; border-radius: 6px 6px 0 0; font-size: 12px; font-weight: 600;
  border: 1px solid transparent; border-bottom: none;
  background: transparent; color: #64748B; cursor: pointer; transition: all .15s;
}
.tab:hover { color: #A0AEC0; }
.tab.active { background: #1a2235; border-color: #2d3748; color: #4F9CF9; }

/* ── 범례 & 필터 ── */
.onto-legend {
  display: flex; align-items: center; flex-wrap: wrap; gap: 10px;
  padding: 7px 20px; border-bottom: 1px solid #1e293b; flex-shrink: 0; min-height: 36px;
}
.legend-filter {
  padding: 2px 10px; border-radius: 5px; font-size: 11px; font-weight: 600;
  border: 1px solid #2d3748; color: #718096; background: transparent; cursor: pointer;
}
.legend-filter.active { border-color: #4F9CF9; color: #4F9CF9; background: rgba(79,156,249,0.08); }
.legend-sep { color: #2d3748; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #94A3B8; }
.legend-dot { width: 9px; height: 9px; border-radius: 50%; }
.legend-hint { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #64748B; }

/* ── 그래프 ── */
.graph-wrap { flex: 1; position: relative; overflow: hidden; }
.graph-chart { width: 100%; height: 100%; }
.graph-loading, .graph-empty {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px; color: #64748B;
}
.empty-icon { color: #2d3748; }
.empty-hint { font-size: 12px; color: #4A5568; }

/* ── 상세 패널 ── */
.detail-panel {
  position: absolute; top: 80px; right: 16px; width: 250px;
  background: #1a2235; border: 1px solid #2d3748; border-radius: 12px;
  padding: 14px; z-index: 10; box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
.dp-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.dp-badge { font-size: 10px; font-weight: 700; color: #fff; padding: 2px 9px; border-radius: 20px; }
.dp-close { background: none; border: none; cursor: pointer; color: #718096; padding: 2px; border-radius: 4px; display: flex; align-items: center; }
.dp-close:hover { color: #E2E8F0; }
.dp-name { font-size: 13px; font-weight: 700; color: #F0F4FF; margin-bottom: 10px; word-break: break-all; }
.dp-props { display: flex; flex-direction: column; gap: 5px; }
.dp-row { display: flex; gap: 8px; font-size: 11px; }
.dp-key { color: #64748B; min-width: 60px; flex-shrink: 0; }
.dp-val { color: #CBD5E1; word-break: break-all; }
.dp-empty { color: #4A5568; font-size: 11px; }

/* ── 토스트 ── */
.enrich-toast {
  position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%);
  display: inline-flex; align-items: center; gap: 7px;
  padding: 8px 18px; border-radius: 8px; font-size: 12px; font-weight: 600;
  background: #1a1f2e; border: 1px solid #8B5CF6; color: #C4B5FD;
  z-index: 9999; box-shadow: 0 4px 16px rgba(0,0,0,0.4); white-space: nowrap;
}

/* ── 트랜지션 ── */
.panel-enter-active, .panel-leave-active { transition: all .2s ease; }
.panel-enter-from, .panel-leave-to { opacity: 0; transform: translateX(20px); }
.toast-enter-active, .toast-leave-active { transition: all .3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 10px); }
</style>
