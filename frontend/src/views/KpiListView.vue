<template>
  <div class="page-container">
    <!-- 개인/팀 KPI 탭 -->
    <div class="kpi-tabs">
      <button
        class="kpi-tab"
        :class="{ active: activeTab === 'personal' }"
        @click="activeTab = 'personal'"
      >내 KPI</button>
      <button
        class="kpi-tab"
        :class="{ active: activeTab === 'team' }"
        @click="activeTab = 'team'"
      >팀 KPI</button>
    </div>

    <!-- 팀 KPI 뷰 -->
    <div v-if="activeTab === 'team'" class="team-kpi-section">
      <div v-if="!orgStore.currentOrg" class="team-empty">
        <p>조직에 참여하면 팀 KPI를 확인할 수 있습니다.</p>
        <router-link to="/onboarding" class="btn-create-org">조직 만들기</router-link>
      </div>
      <div v-else-if="teamKpis.length === 0" class="team-empty">
        <p>등록된 팀 KPI가 없습니다.</p>
        <el-button type="primary" size="small" @click="openCreateTeamDialog">팀 KPI 추가</el-button>
      </div>
      <div v-else class="kpi-list-grid">
        <div v-for="kpi in teamKpis" :key="kpi.id" class="kpi-list-card team-card">
          <div class="colorbar" :style="{ background: kpi.category?.color || '#4F46E5' }"></div>
          <div class="card-body">
            <div class="kpi-name">{{ kpi.name }}</div>
            <div class="kpi-meta">
              <span>{{ kpi.category?.name }}</span>
              <span class="dot">·</span>
              <span>{{ freqLabel(kpi.frequency) }}</span>
            </div>
          </div>
          <el-tag type="info" size="small">팀</el-tag>
        </div>
      </div>
    </div>

    <!-- 개인 KPI 뷰 (기존) -->
    <template v-if="activeTab === 'personal'">

    <!-- 액션 바 -->
    <div class="action-bar">
      <div class="filters">
        <!-- 검색 -->
        <el-input
          v-model="searchQuery"
          placeholder="KPI 이름 검색..."
          :prefix-icon="Search"
          clearable
          style="width:200px"
        />
        <el-select v-model="filterStatus" placeholder="상태" clearable style="width:120px">
          <el-option label="전체" value="" />
          <el-option label="활성" value="ACTIVE" />
          <el-option label="일시정지" value="PAUSED" />
          <el-option label="완료" value="COMPLETED" />
        </el-select>
        <el-select v-model="filterCategory" placeholder="카테고리" clearable style="width:150px">
          <el-option
            v-for="cat in store.categories"
            :key="cat.id" :label="cat.name" :value="cat.id"
          />
        </el-select>
      </div>
      <div class="flex gap-2">
        <el-button size="default" @click="exportCsv">
          <Download :size="15" class="mr-1" /> CSV 내보내기
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <Plus :size="15" class="mr-1" /> KPI 추가
        </el-button>
      </div>
    </div>

    <!-- 결과 수 -->
    <div class="result-count" v-if="!store.loading">
      {{ filteredKpis.length }}개의 KPI
      <span v-if="searchQuery" class="text-blue-500"> — "{{ searchQuery }}" 검색 결과</span>
      <span v-if="isFilterActive" class="filter-active-hint">
        · 필터 적용 중 — 순서 변경을 하려면 필터를 해제하세요
      </span>
    </div>

    <!-- 스켈레톤 로딩 -->
    <div v-if="store.loading && store.kpis.length === 0" class="kpi-list-grid">
      <div v-for="i in 5" :key="i" class="kpi-list-card" style="display:flex;align-items:flex-start;gap:14px">
        <div class="sk-colorbar"></div>
        <div style="flex:1">
          <div class="sk-kpi-name mb-2"></div>
          <div class="sk-kpi-meta mb-2"></div>
          <div class="sk-kpi-desc"></div>
        </div>
        <div style="display:flex;gap:6px;align-items:center">
          <div class="sk-tag"></div>
          <div class="sk-btn"></div>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else-if="filteredKpis.length === 0" class="empty-state">
      <BarChart3 :size="48" class="text-slate-300 mb-3" />
      <p class="text-slate-500 font-medium">
        {{ searchQuery ? `"${searchQuery}"에 해당하는 KPI가 없습니다` : '등록된 KPI가 없습니다' }}
      </p>
      <el-button type="primary" class="mt-4" @click="openCreateDialog" v-if="!searchQuery">
        첫 번째 KPI 만들기
      </el-button>
    </div>

    <!-- KPI 카드 목록 -->
    <div v-else class="kpi-list-grid">
      <div
        v-for="(kpi, idx) in filteredKpis"
        :key="kpi.id"
        class="kpi-list-card"
        :draggable="!isFilterActive"
        @dragstart="onDragStart($event, idx)"
        @dragover.prevent="onDragOver($event, idx)"
        @drop="onDrop($event, idx)"
        @dragend="onDragEnd"
        :class="{ 'drag-over': dragOverIdx === idx && !isFilterActive, 'is-dragging': dragStartIdx === idx }"
      >
        <div class="kpi-list-header">
          <div class="kpi-left">
            <div class="kpi-color-bar" :style="{ backgroundColor: kpi.category?.color || '#4F46E5' }"></div>
            <div>
              <div class="kpi-name">{{ kpi.name }}</div>
              <div class="kpi-meta">
                <span class="meta-badge"
                  :style="{ background: (kpi.category?.color||'#4F46E5')+'20', color: kpi.category?.color||'#4F46E5' }">
                  {{ kpi.category?.name }}
                </span>
                <span class="meta-tag">{{ frequencyLabel(kpi.frequency) }}</span>
                <span class="meta-tag">{{ kpiTypeLabel(kpi.kpiType) }}</span>
              </div>
            </div>
          </div>
          <div class="kpi-actions">
            <!-- 즐겨찾기 (CAT10) -->
            <el-tooltip :content="isFavorite(kpi.id) ? '즐겨찾기 해제' : '즐겨찾기 추가'" placement="top">
              <button class="fav-btn" :class="{ active: isFavorite(kpi.id) }" @click.stop="toggleFavorite(kpi.id)">
                <Star
                  :size="15"
                  :fill="isFavorite(kpi.id) ? '#F59E0B' : 'none'"
                  :color="isFavorite(kpi.id) ? '#F59E0B' : '#94A3B8'"
                />
              </button>
            </el-tooltip>
            <el-tag :type="statusTagType(kpi.status)" size="small">{{ statusLabel(kpi.status) }}</el-tag>
            <!-- 정렬 버튼 (필터 적용 중 비활성화) -->
            <el-tooltip
              :content="isFilterActive ? '필터 해제 후 순서를 변경할 수 있습니다' : '순서 변경'"
              placement="top"
              :disabled="!isFilterActive"
            >
              <div class="sort-btns">
                <button
                  class="sort-btn"
                  :disabled="isFilterActive || store.kpis.findIndex(k => k.id === kpi.id) === 0"
                  @click="moveSortOrder(kpi, -1)"
                  title="위로"
                >▲</button>
                <button
                  class="sort-btn"
                  :disabled="isFilterActive || store.kpis.findIndex(k => k.id === kpi.id) === store.kpis.length - 1"
                  @click="moveSortOrder(kpi, 1)"
                  title="아래로"
                >▼</button>
              </div>
            </el-tooltip>
            <el-dropdown @command="onCommand($event, kpi)">
              <el-button text circle size="small">
                <MoreHorizontal :size="16" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">수정</el-dropdown-item>
                  <el-dropdown-item command="detail">상세 보기</el-dropdown-item>
                  <el-dropdown-item command="copy">복사하기</el-dropdown-item>
                  <el-dropdown-item command="pause" v-if="kpi.status === 'ACTIVE'">일시정지</el-dropdown-item>
                  <el-dropdown-item command="activate" v-if="kpi.status === 'PAUSED'">활성화</el-dropdown-item>
                  <el-dropdown-item command="delete" style="color:#EF4444">삭제</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div v-if="kpi.description" class="kpi-description">{{ kpi.description }}</div>

        <div class="kpi-target">
          <span v-if="kpi.kpiType !== 'BOOLEAN'">
            목표: <strong>{{ kpi.targetValue?.toLocaleString() }} {{ kpi.unit }}</strong>
          </span>
          <span v-else>타입: <strong>달성 여부</strong></span>
          <span class="text-slate-400">| 시작: {{ kpi.startDate }}
            <template v-if="kpi.endDate"> ~ {{ kpi.endDate }}</template>
          </span>
        </div>
      </div>
    </div>

    <!-- KPI 생성/수정 다이얼로그 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingKpi ? 'KPI 수정' : 'KPI 추가'"
      width="560px"
      destroy-on-close
    >
      <KpiForm
        :kpi="editingKpi"
        :categories="store.categories"
        @submit="onFormSubmit"
        @cancel="dialogVisible = false"
      />
    </el-dialog>
    </template> <!-- end personal tab -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useKpiStore } from '@/store/kpiStore'
import { useOrgStore } from '@/store/orgStore'
import { kpiApi, exportApi } from '@/api/kpiApi'
import api from '@/api/axios'
import KpiForm from '@/components/kpi/KpiForm.vue'
import { Plus, BarChart3, MoreHorizontal, Search, Download, Star } from 'lucide-vue-next'
import dayjs from 'dayjs'

const store    = useKpiStore()
const orgStore = useOrgStore()
const router   = useRouter()

const activeTab  = ref('personal')
const teamKpis   = ref([])

async function fetchTeamKpis() {
  if (!orgStore.currentOrg) return
  const res = await api.get(`/kpis/team?orgId=${orgStore.currentOrg.id}`)
  teamKpis.value = res.data || []
}

function freqLabel(f) {
  return f === 'DAILY' ? '매일' : f === 'WEEKLY' ? '매주' : '매월'
}

function openCreateTeamDialog() {
  // 팀 KPI 생성은 일반 KPI 폼에서 scope=TEAM으로 처리
  openCreateDialog()
}

const dialogVisible  = ref(false)
const editingKpi     = ref(null)
const filterStatus   = ref('')
const filterCategory = ref('')
const searchQuery    = ref('')

// 필터가 하나라도 활성화되면 정렬 버튼 비활성화
const isFilterActive = computed(() =>
  !!filterStatus.value || !!filterCategory.value || !!searchQuery.value
)

// ── 드래그앤드롭 정렬 (CAT10) ──
const dragStartIdx = ref(null)
const dragOverIdx  = ref(null)

function onDragStart(e, idx) {
  dragStartIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver(e, idx) {
  dragOverIdx.value = idx
}

async function onDrop(e, idx) {
  if (dragStartIdx.value === null || dragStartIdx.value === idx) return
  const list = [...filteredKpis.value]
  const [moved] = list.splice(dragStartIdx.value, 1)
  list.splice(idx, 0, moved)
  // sortOrder 재할당 후 저장
  const updates = list.map((k, i) => ({ ...k, sortOrder: i }))
  for (const k of updates) {
    await store.updateKpi(k.id, { ...k })
  }
  await store.fetchKpis()
}

function onDragEnd() {
  dragStartIdx.value = null
  dragOverIdx.value = null
}

// ── 즐겨찾기 (CAT10) ──
const FAV_KEY = 'workoop-favorites'
const favorites = ref(new Set(JSON.parse(localStorage.getItem(FAV_KEY) || '[]')))

function isFavorite(id) { return favorites.value.has(id) }

function toggleFavorite(id) {
  const s = new Set(favorites.value)
  s.has(id) ? s.delete(id) : s.add(id)
  favorites.value = s
  localStorage.setItem(FAV_KEY, JSON.stringify([...s]))
}

const filteredKpis = computed(() => {
  return store.kpis.filter(k => {
    if (filterStatus.value   && k.status !== filterStatus.value)         return false
    if (filterCategory.value && k.categoryId !== filterCategory.value)   return false
    if (searchQuery.value && !k.name.toLowerCase().includes(searchQuery.value.toLowerCase())) return false
    return true
  })
})

onMounted(async () => {
  await Promise.all([store.fetchKpis(), store.fetchCategories()])
  fetchTeamKpis()
})

function openCreateDialog() {
  editingKpi.value = null
  dialogVisible.value = true
}

async function onCommand(cmd, kpi) {
  if (cmd === 'edit') {
    editingKpi.value = { ...kpi, categoryId: kpi.categoryId }
    dialogVisible.value = true
  } else if (cmd === 'detail') {
    router.push(`/kpis/${kpi.id}`)
  } else if (cmd === 'copy') {
    await kpiApi.copy(kpi.id)
    ElMessage.success(`"${kpi.name}" KPI가 복사되었습니다.`)
    store.fetchKpis()
  } else if (cmd === 'pause') {
    await kpiApi.updateStatus(kpi.id, 'PAUSED')
    ElMessage.success('일시정지되었습니다.')
    store.fetchKpis()
  } else if (cmd === 'activate') {
    await kpiApi.updateStatus(kpi.id, 'ACTIVE')
    ElMessage.success('활성화되었습니다.')
    store.fetchKpis()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm(`"${kpi.name}" KPI를 삭제하시겠습니까?`, '삭제 확인', {
      confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning'
    })
    store.deleteKpi(kpi.id)
  }
}

async function onFormSubmit(data) {
  if (editingKpi.value) {
    await store.updateKpi(editingKpi.value.id, data)
  } else {
    await store.createKpi(data)
  }
  dialogVisible.value = false
}

// 정렬 순서 변경 — 전체(필터되지 않은) 목록 기준으로 인접 KPI와 swap
// 필터 적용 중에는 버튼이 비활성화되므로 호출되지 않음
async function moveSortOrder(kpi, direction) {
  const fullList = store.kpis  // 필터 무관 전체 목록
  const idx = fullList.findIndex(k => k.id === kpi.id)
  const target = fullList[idx + direction]
  if (!target) return

  const newSortA = target.sortOrder ?? idx + direction
  const newSortB = kpi.sortOrder ?? idx

  await Promise.all([
    kpiApi.update(kpi.id,    { ...kpi,    sortOrder: newSortA }),
    kpiApi.update(target.id, { ...target, sortOrder: newSortB })
  ])
  store.fetchKpis()
}

function exportCsv() {
  exportApi.csv(
    dayjs().subtract(1, 'month').format('YYYY-MM-DD'),
    dayjs().format('YYYY-MM-DD')
  )
  ElMessage.success('CSV 파일을 다운로드합니다.')
}

const frequencyLabel  = (f) => ({ DAILY:'매일', WEEKLY:'매주', MONTHLY:'매월' }[f] ?? f)
const kpiTypeLabel    = (t) => ({ NUMERIC:'수치', PERCENTAGE:'퍼센트', BOOLEAN:'달성여부' }[t] ?? t)
const statusLabel     = (s) => ({ ACTIVE:'활성', PAUSED:'일시정지', COMPLETED:'완료' }[s] ?? s)
const statusTagType   = (s) => ({ ACTIVE:'success', PAUSED:'warning', COMPLETED:'info' }[s] ?? '')
</script>

<style scoped>
.page-container { padding: 24px 28px; }

.action-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; gap: 12px; flex-wrap: wrap;
}
.filters { display: flex; gap: 8px; flex-wrap: wrap; }

.kpi-tabs {
  display: flex; gap: 4px; margin-bottom: 16px;
  border-bottom: 1px solid var(--border-color); padding-bottom: 0;
}
.kpi-tab {
  padding: 8px 16px; background: transparent;
  border: none; border-bottom: 2px solid transparent;
  margin-bottom: -1px; font-size: 14px; font-weight: 600;
  color: var(--text-muted); cursor: pointer; transition: all var(--transition-fast);
  font-family: inherit;
}
.kpi-tab.active { color: var(--color-project); border-bottom-color: var(--color-project); }
.kpi-tab:hover:not(.active) { color: var(--text-secondary); }

.team-kpi-section { padding-top: 8px; }
.team-empty {
  text-align: center; padding: 48px 24px;
  background: var(--bg-card); border-radius: var(--radius-lg);
  border: 1px dashed var(--border-color); color: var(--text-muted);
}
.team-empty p { margin-bottom: 12px; }
.btn-create-org {
  display: inline-block; padding: 7px 16px;
  background: var(--color-project); color: white; border-radius: var(--radius-md);
  font-size: 13px; font-weight: 600; text-decoration: none;
}
.team-card { border-left: 3px solid var(--color-project); }
.card-body { flex: 1; }
.kpi-name { font-weight: 600; color: var(--text-primary); margin-bottom: 4px; }
.kpi-meta { font-size: 12px; color: var(--text-muted); display: flex; gap: 4px; }
.dot { color: var(--border-color); }

.result-count { font-size: 13px; color: var(--text-secondary); margin-bottom: 12px; }

.kpi-list-grid { display: flex; flex-direction: column; gap: 8px; }

.kpi-list-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 14px 18px;
  box-shadow: var(--shadow-xs);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.kpi-list-card:hover { border-color: var(--gray-300); box-shadow: var(--shadow-sm); }

.kpi-list-header { display: flex; align-items: flex-start; justify-content: space-between; }
.kpi-left { display: flex; align-items: flex-start; gap: 12px; }

.kpi-color-bar { width: 3px; height: 40px; border-radius: 3px; flex-shrink: 0; margin-top: 2px; }
.kpi-name { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 5px; letter-spacing: -0.01em; }

.kpi-meta { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; }
.meta-badge { font-size: 11px; font-weight: 600; padding: 2px 7px; border-radius: var(--radius-sm); }
.meta-tag   { font-size: 11px; color: var(--text-secondary); background: var(--bg-hover); padding: 2px 7px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); }

.kpi-actions { display: flex; align-items: center; gap: 5px; }

.sort-btns { display: flex; flex-direction: column; gap: 1px; }
.sort-btn {
  width: 20px; height: 15px; padding: 0;
  background: var(--bg-hover); border: none; border-radius: var(--radius-xs);
  font-size: 9px; color: var(--text-secondary); cursor: pointer; line-height: 1;
  transition: all var(--transition-fast); font-family: inherit;
}
.sort-btn:hover:not(:disabled) { background: var(--border-color); color: var(--text-primary); }
.sort-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.filter-active-hint { color: var(--color-warning); font-size: 12px; font-weight: 500; }

.kpi-description { font-size: 13px; color: var(--text-secondary); margin-top: 7px; padding-left: 15px; line-height: 1.5; }
.kpi-target {
  font-size: 12px; color: var(--text-secondary); margin-top: 7px; padding-left: 15px;
  display: flex; gap: 8px; flex-wrap: wrap;
}

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; }

.kpi-list-card[draggable="true"] { cursor: grab; }
.kpi-list-card[draggable="true"]:active { cursor: grabbing; }
.kpi-list-card.drag-over {
  border-color: var(--color-project) !important;
  box-shadow: 0 0 0 3px rgba(48,127,226,0.15);
  transform: translateY(-1px);
}
.kpi-list-card.is-dragging { opacity: 0.5; }

.fav-btn {
  background: none; border: none; cursor: pointer;
  font-size: 16px; line-height: 1; padding: 2px;
  transition: transform var(--transition-fast); color: var(--text-muted);
}
.fav-btn:hover { transform: scale(1.2); }
.fav-btn.active { color: #FBBF24; }

.sk-colorbar, .sk-kpi-name, .sk-kpi-meta, .sk-kpi-desc, .sk-tag, .sk-btn {
  background: linear-gradient(90deg, var(--bg-hover) 25%, var(--border-light) 50%, var(--bg-hover) 75%);
  background-size: 400% 100%; animation: shimmer 1.5s ease-in-out infinite; border-radius: var(--radius-sm);
}
.sk-colorbar { width: 3px; height: 40px; border-radius: 3px; flex-shrink: 0; margin-top: 2px; }
.sk-kpi-name { height: 18px; width: 55%; }
.sk-kpi-meta { height: 13px; width: 38%; }
.sk-kpi-desc { height: 12px; width: 70%; }
.sk-tag      { height: 20px; width: 44px; }
.sk-btn      { height: 26px; width: 26px; border-radius: 50%; }
.mb-2        { margin-bottom: 8px; }
</style>
