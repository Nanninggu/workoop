<template>
  <div class="page-container">

    <!-- 날짜 헤더 -->
    <div class="date-header">
      <div class="date-nav-block">
        <!-- 주/월 이동 + 일 이동 -->
        <div class="date-selector">
          <!-- 이전 월 -->
          <el-tooltip content="이전 달" placement="top">
            <el-button circle size="small" class="nav-jump-btn" @click="changeMonth(-1)">
              <ChevronsLeft :size="14" />
            </el-button>
          </el-tooltip>
          <!-- 이전 주 -->
          <el-tooltip content="이전 주" placement="top">
            <el-button circle size="small" class="nav-jump-btn" @click="changeWeek(-1)">
              <ChevronLeft :size="14" />
            </el-button>
          </el-tooltip>

          <el-date-picker
            v-model="selectedDate"
            type="date"
            format="YYYY년 MM월 DD일"
            value-format="YYYY-MM-DD"
            :clearable="false"
            style="width:200px"
            @change="loadRecords"
          />

          <!-- 다음 주 -->
          <el-tooltip content="다음 주" placement="top">
            <el-button circle size="small" class="nav-jump-btn" @click="changeWeek(1)" :disabled="isThisWeekOrLater">
              <ChevronRight :size="14" />
            </el-button>
          </el-tooltip>
          <!-- 다음 월 -->
          <el-tooltip content="다음 달" placement="top">
            <el-button circle size="small" class="nav-jump-btn" @click="changeMonth(1)" :disabled="isThisMonthOrLater">
              <ChevronsRight :size="14" />
            </el-button>
          </el-tooltip>

          <el-button size="small" plain @click="goToday" v-if="!isToday" class="today-btn">오늘로</el-button>
          <span class="date-label" v-if="isToday">오늘</span>
        </div>

        <!-- 날짜 컨텍스트 정보 -->
        <div class="date-context">
          <span class="date-context-week">{{ weekLabel }}</span>
          <span class="date-context-month">{{ monthLabel }}</span>
        </div>
      </div>

      <!-- 진행 현황 + 필터 + 액션 -->
      <div class="date-summary" v-if="!store.loading">
        <div class="summary-pill" :class="allDone ? 'all-done' : ''">
          <CheckCircle2 :size="14" />
          {{ recordedCount }} / {{ displayKpisCount }} 완료
        </div>

        <!-- 오늘의 KPI 필터 토글 (REC-1) -->
        <el-tooltip :content="showOnlyDue ? '미완료 KPI만 보는 중 — 클릭하여 전체 보기' : '미완료 KPI만 보기'" placement="top">
          <button
            class="due-filter-btn"
            :class="{ active: showOnlyDue }"
            @click="toggleDueFilter"
          >
            <Filter :size="13" />
            {{ showOnlyDue ? '미완료만' : '전체 보기' }}
            <span v-if="dueCount > 0 && !showOnlyDue" class="due-badge">{{ dueCount }}</span>
          </button>
        </el-tooltip>

        <div class="date-summary-actions">
          <el-button size="small" type="success" @click="saveAll" :loading="savingAll">
            <Save :size="14" class="mr-1" /> 전체 저장
          </el-button>
          <el-button size="small" type="primary" plain @click="openKpiAddDialog">
            <Plus :size="14" class="mr-1" /> KPI 추가
          </el-button>
        </div>
      </div>
    </div>

    <!-- 로딩 -->
    <div v-if="store.loading" class="flex justify-center items-center h-64">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <template v-else>
      <!-- 미완료 KPI 없을 때 (필터 활성화 상태) -->
      <div v-if="showOnlyDue && displayKpisByCategory.length === 0" class="all-clear-state">
        <div class="all-clear-icon">🎉</div>
        <div class="all-clear-title">모든 KPI를 완료했습니다!</div>
        <div class="all-clear-sub">{{ weekLabel }}의 KPI 입력이 모두 완료되었습니다.</div>
        <el-button size="small" plain @click="showOnlyDue = false" class="mt-3">전체 보기</el-button>
      </div>

      <!-- 빈 상태 (KPI 없음) -->
      <div v-else-if="displayKpisByCategory.length === 0" class="empty-state">
        <ListChecks :size="48" class="text-slate-300 mb-3" />
        <p class="text-slate-500 font-medium">활성 KPI가 없습니다.</p>
        <el-button type="primary" class="mt-4" @click="openKpiAddDialog">KPI 추가하기</el-button>
      </div>

      <div v-else class="record-sections">
        <div
          v-for="group in displayKpisByCategory"
          :key="group.categoryId"
          class="record-section"
        >
          <!-- 카테고리 헤더 -->
          <div class="section-header" @click="toggleGroup(group.categoryId)" style="cursor:pointer">
            <div class="cat-title">
              <span class="category-dot" :style="{ backgroundColor: group.color }"></span>
              <span class="category-name">{{ group.categoryName }}</span>
              <span class="cat-count">{{ group.kpis.length }}개</span>
            </div>
            <div class="cat-header-right">
              <span class="cat-done">
                {{ group.kpis.filter(k => getRecord(k.id)?.id).length }} / {{ group.kpis.length }} 완료
              </span>
              <button class="collapse-btn">
                <ChevronUp v-if="!collapsedGroups.has(group.categoryId)" :size="15" />
                <ChevronDown v-else :size="15" />
              </button>
            </div>
          </div>

          <div class="record-cards" v-show="!collapsedGroups.has(group.categoryId)">
            <div
              v-for="kpi in group.kpis"
              :key="kpi.id"
              class="record-card"
              :class="{
                recorded: !!getRecord(kpi.id)?.id,
                'period-done': isPeriodDone(kpi) && !getRecord(kpi.id)?.id
              }"
            >
              <!-- 카드 상단 -->
              <div class="record-card-top">
                <div class="record-kpi-info">
                  <div class="record-kpi-name">{{ kpi.name }}</div>
                  <div class="record-kpi-meta">
                    {{ frequencyLabel(kpi.frequency) }}
                    <template v-if="kpi.kpiType !== 'BOOLEAN'">
                      · 목표: <strong>{{ kpi.targetValue?.toLocaleString() }} {{ kpi.unit }}</strong>
                    </template>
                  </div>
                </div>
                <div class="card-top-actions">
                  <!-- 날짜 기록 완료 배지 -->
                  <el-tag v-if="getRecord(kpi.id)?.id" type="success" size="small" class="done-tag">완료</el-tag>

                  <!-- 주기 완료 배지 (REC-2): 날짜 기록 없이 기간 내 이미 기록된 경우 -->
                  <el-tooltip
                    v-else-if="isPeriodDone(kpi)"
                    :content="periodDoneTooltip(kpi)"
                    placement="top"
                  >
                    <el-tag type="info" size="small" class="period-done-tag">
                      {{ periodDoneLabel(kpi) }}
                    </el-tag>
                  </el-tooltip>

                  <!-- KPI 수정 버튼 -->
                  <el-tooltip content="KPI 설정 수정">
                    <button class="icon-btn" @click="openKpiEditDialog(kpi)">
                      <Settings :size="14" />
                    </button>
                  </el-tooltip>
                </div>
              </div>

              <!-- 기간 완료 안내 (REC-2): 이번 주/월 이미 기록됨 -->
              <div v-if="isPeriodDone(kpi) && !getRecord(kpi.id)?.id" class="period-done-banner">
                <CalendarCheck :size="13" />
                {{ periodDoneTooltip(kpi) }}. 이 날짜에도 추가 입력할 수 있습니다.
              </div>

              <!-- 기존 저장값 표시 -->
              <div v-if="getRecord(kpi.id)?.id" class="saved-value-row">
                <span class="saved-label">저장된 값:</span>
                <span class="saved-value" v-if="kpi.kpiType !== 'BOOLEAN'">
                  {{ getRecord(kpi.id).actualValue?.toLocaleString() }} {{ kpi.unit }}
                </span>
                <span class="saved-value" v-else>
                  {{ getRecord(kpi.id).booleanValue ? '✅ 달성' : '❌ 미달성' }}
                </span>
                <span v-if="getRecord(kpi.id).note" class="saved-note">— {{ getRecord(kpi.id).note }}</span>
              </div>

              <!-- 입력 영역 -->
              <template v-if="recordValues[kpi.id]">

                <!-- BOOLEAN -->
                <div v-if="kpi.kpiType === 'BOOLEAN'" class="record-input-area">
                  <el-switch
                    v-model="recordValues[kpi.id].booleanValue"
                    active-text="달성"
                    inactive-text="미달성"
                    active-color="#10B981"
                    size="large"
                  />
                </div>

                <!-- NUMERIC / PERCENTAGE -->
                <div v-else class="record-input-area">
                  <div class="input-with-unit">
                    <el-input-number
                      v-model="recordValues[kpi.id].actualValue"
                      :min="0"
                      :precision="kpi.kpiType === 'PERCENTAGE' ? 1 : 0"
                      :max="kpi.kpiType === 'PERCENTAGE' ? 100 : undefined"
                      controls-position="right"
                      style="flex:1"
                    />
                    <span v-if="kpi.unit" class="unit-label">{{ kpi.unit }}</span>
                  </div>
                  <!-- 달성률 프리뷰 -->
                  <div
                    v-if="kpi.targetValue && recordValues[kpi.id].actualValue != null"
                    class="rate-preview"
                  >
                    <el-progress
                      :percentage="Math.min(recordValues[kpi.id].actualValue / kpi.targetValue * 100, 100)"
                      :color="progressColor(recordValues[kpi.id].actualValue / kpi.targetValue * 100)"
                      :show-text="false"
                      :stroke-width="6"
                    />
                    <span class="rate-text">
                      {{ Math.min(Math.round(recordValues[kpi.id].actualValue / kpi.targetValue * 100), 100) }}%
                    </span>
                  </div>
                </div>

                <!-- 메모 (REC-4): 이전 메모 자동완성 -->
                <div class="record-note-area">
                  <el-autocomplete
                    v-model="recordValues[kpi.id].note"
                    :fetch-suggestions="(q, cb) => getNoteAutocomplete(kpi.id, q, cb)"
                    placeholder="메모 (선택 사항) — 이전 메모에서 선택 가능"
                    size="small"
                    clearable
                    style="width:100%"
                    @focus="loadNoteSuggestions(kpi.id)"
                    :trigger-on-focus="true"
                  >
                    <template #prefix>
                      <MessageSquare :size="12" style="color:#94A3B8" />
                    </template>
                  </el-autocomplete>
                </div>

                <!-- 액션 버튼 -->
                <div class="record-actions">
                  <el-popconfirm
                    v-if="getRecord(kpi.id)?.id"
                    title="이 기록을 삭제하시겠습니까?"
                    confirm-button-text="삭제"
                    cancel-button-text="취소"
                    confirm-button-type="danger"
                    @confirm="deleteRecord(kpi)"
                  >
                    <template #reference>
                      <el-button size="small" type="danger" plain :loading="deleting[kpi.id]">
                        <Trash2 :size="13" class="mr-1" /> 삭제
                      </el-button>
                    </template>
                  </el-popconfirm>

                  <el-button
                    size="small"
                    type="primary"
                    @click="saveRecord(kpi)"
                    :loading="saving[kpi.id]"
                    style="margin-left: auto"
                  >
                    <Check :size="13" class="mr-1" />
                    {{ getRecord(kpi.id)?.id ? '수정 저장' : '저장' }}
                  </el-button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- KPI 생성/수정 다이얼로그 -->
    <el-dialog
      v-model="kpiDialogVisible"
      :title="editingKpi ? 'KPI 수정' : 'KPI 추가'"
      width="560px"
      destroy-on-close
    >
      <KpiForm
        :kpi="editingKpi"
        :categories="store.categories"
        @submit="onKpiFormSubmit"
        @cancel="kpiDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
import { useKpiStore } from '@/store/kpiStore'
import { recordApi } from '@/api/kpiApi'
import { ElMessage } from 'element-plus'
import KpiForm from '@/components/kpi/KpiForm.vue'
import {
  ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight,
  ListChecks, CheckCircle2, Plus, Settings, Trash2, Check,
  ChevronDown, ChevronUp, Save, Filter, MessageSquare, CalendarCheck
} from 'lucide-vue-next'

dayjs.extend(isoWeek)

const store  = useKpiStore()
const route  = useRoute()
const router = useRouter()

// URL query param 동기화 (ux4)
const selectedDate    = ref(route.query.date || dayjs().format('YYYY-MM-DD'))
const recordValues    = reactive({})
const existingRecords = ref([])
const saving   = reactive({})
const deleting = reactive({})
const savingAll = ref(false)

// 카테고리 섹션 접기/펼치기 (ux5)
const collapsedGroups = ref(new Set())
const toggleGroup = (catId) => {
  collapsedGroups.value.has(catId)
    ? collapsedGroups.value.delete(catId)
    : collapsedGroups.value.add(catId)
  collapsedGroups.value = new Set(collapsedGroups.value)
}

// 날짜 변경 시 URL 동기화
watch(selectedDate, (date) => {
  router.replace({ query: { ...route.query, date } })
})

// KPI 추가/수정 다이얼로그
const kpiDialogVisible = ref(false)
const editingKpi = ref(null)

// ── REC-1: 미완료 KPI 필터 ──
const showOnlyDue = ref(false)

// 대시보드 KPI 요약 맵 (주기별 완료 여부)
const kpiSummaryMap = computed(() => {
  const map = {}
  ;(store.dashboard?.kpiSummaries || []).forEach(s => { map[s.kpiId] = s })
  return map
})

// 주기별 완료 여부 (REC-2)
function isPeriodDone(kpi) {
  const summary = kpiSummaryMap.value[kpi.id]
  if (!summary) return false
  return summary.recordedInPeriod && !getRecord(kpi.id)?.id
}

function periodDoneLabel(kpi) {
  return { DAILY: '오늘 완료', WEEKLY: '이번 주 완료', MONTHLY: '이번 달 완료' }[kpi.frequency] || '기간 완료'
}

function periodDoneTooltip(kpi) {
  const summary = kpiSummaryMap.value[kpi.id]
  if (!summary) return ''
  const labels = {
    DAILY: '오늘 이미 다른 날짜에 기록됨',
    WEEKLY: '이번 주에 이미 기록이 있습니다',
    MONTHLY: '이번 달에 이미 기록이 있습니다'
  }
  return labels[kpi.frequency] || '기간 내 기록 있음'
}

// 미완료 KPI 수: 날짜 기록 없고 기간 완료도 아닌 KPI
const dueCount = computed(() => {
  return store.activeKpis.filter(k => {
    const dateRecorded = !!getRecord(k.id)?.id
    const periodRecorded = kpiSummaryMap.value[k.id]?.recordedInPeriod
    return !dateRecorded && !periodRecorded
  }).length
})

function toggleDueFilter() {
  showOnlyDue.value = !showOnlyDue.value
  if (showOnlyDue.value && !store.dashboard) {
    store.fetchDashboard()
  }
}

// ── 날짜 계산 ──
const isToday = computed(() => selectedDate.value === dayjs().format('YYYY-MM-DD'))

const isThisWeekOrLater = computed(() => {
  const nextWeek = dayjs(selectedDate.value).add(7, 'day')
  return nextWeek.isAfter(dayjs(), 'day')
})

const isThisMonthOrLater = computed(() => {
  const nextMonth = dayjs(selectedDate.value).add(1, 'month')
  return nextMonth.isAfter(dayjs(), 'day')
})

const weekLabel = computed(() => {
  const d = dayjs(selectedDate.value)
  const weekStart = d.isoWeekday(1).format('M/D')
  const weekEnd = d.isoWeekday(7).format('M/D')
  const diff = d.isoWeek() - dayjs().isoWeek()
  const weekText = diff === 0 ? '이번 주' : diff === -1 ? '지난 주' : diff === 1 ? '다음 주' : `${d.isoWeek()}주차`
  return `${weekText} (${weekStart}~${weekEnd})`
})

const monthLabel = computed(() => {
  const d = dayjs(selectedDate.value)
  const diff = d.diff(dayjs(), 'month')
  if (diff === 0) return `${d.month() + 1}월`
  if (diff === -1) return '지난 달'
  return `${d.year()}년 ${d.month() + 1}월`
})

// ── KPI 그룹핑 ──
const totalCount    = computed(() => store.activeKpis.length)
const recordedCount = computed(() => store.activeKpis.filter(k => getRecord(k.id)?.id).length)
const allDone       = computed(() => totalCount.value > 0 && recordedCount.value === totalCount.value)

function buildCategoryGroups(kpiList) {
  const groups = {}
  kpiList.forEach(kpi => {
    const catId = kpi.categoryId
    if (!groups[catId]) {
      groups[catId] = {
        categoryId: catId,
        categoryName: kpi.category?.name || '기타',
        color: kpi.category?.color || '#4F46E5',
        kpis: []
      }
    }
    groups[catId].kpis.push(kpi)
  })
  return Object.values(groups).sort((a, b) =>
    a.categoryName.localeCompare(b.categoryName, 'ko')
  )
}

// 필터 적용한 그룹 (REC-1)
const displayKpisByCategory = computed(() => {
  let kpiList = store.activeKpis
  if (showOnlyDue.value) {
    kpiList = kpiList.filter(k => {
      const dateRecorded = !!getRecord(k.id)?.id
      const periodRecorded = kpiSummaryMap.value[k.id]?.recordedInPeriod
      return !dateRecorded && !periodRecorded
    })
  }
  return buildCategoryGroups(kpiList)
})

const displayKpisCount = computed(() => {
  if (showOnlyDue.value) return dueCount.value
  return totalCount.value
})

// ── 기록 관리 ──
function getRecord(kpiId) {
  return existingRecords.value.find(r => r.kpiId === kpiId) || null
}

function initRecordValues() {
  store.activeKpis.forEach(kpi => {
    const existing = getRecord(kpi.id)
    recordValues[kpi.id] = {
      actualValue: existing?.actualValue != null ? Number(existing.actualValue) : null,
      booleanValue: existing?.booleanValue ?? false,
      note: existing?.note ?? ''
    }
  })
}

async function loadRecords() {
  await store.fetchKpis()
  store.activeKpis.forEach(kpi => {
    if (!recordValues[kpi.id]) {
      recordValues[kpi.id] = { actualValue: null, booleanValue: false, note: '' }
    }
  })
  const res = await recordApi.getByDate(selectedDate.value)
  existingRecords.value = res.data || []
  initRecordValues()
}

async function saveRecord(kpi) {
  saving[kpi.id] = true
  try {
    const val = recordValues[kpi.id]
    await store.saveRecord({
      kpiId: kpi.id,
      actualValue: kpi.kpiType !== 'BOOLEAN' ? val.actualValue : null,
      booleanValue: kpi.kpiType === 'BOOLEAN' ? val.booleanValue : null,
      recordedDate: selectedDate.value,
      note: val.note || null
    })
    // 메모 캐시 초기화 (새로 저장하면 새 메모 포함시키기 위해)
    delete noteSuggestionsCache[kpi.id]
    await loadRecords()
  } finally {
    saving[kpi.id] = false
  }
}

async function deleteRecord(kpi) {
  const record = getRecord(kpi.id)
  if (!record?.id) return
  deleting[kpi.id] = true
  try {
    await recordApi.delete(record.id)
    ElMessage.success('기록이 삭제되었습니다.')
    await loadRecords()
  } finally {
    deleting[kpi.id] = false
  }
}

async function saveAll() {
  savingAll.value = true
  const targets = store.activeKpis.filter(kpi => {
    const val = recordValues[kpi.id]
    if (!val) return false
    if (kpi.kpiType === 'BOOLEAN') return true
    return val.actualValue !== null && val.actualValue !== undefined
  })
  if (targets.length === 0) {
    ElMessage.warning('저장할 값이 없습니다. 값을 먼저 입력해주세요.')
    savingAll.value = false
    return
  }
  try {
    await Promise.all(targets.map(kpi => {
      const val = recordValues[kpi.id]
      return store.saveRecord({
        kpiId: kpi.id,
        actualValue: kpi.kpiType !== 'BOOLEAN' ? val.actualValue : null,
        booleanValue: kpi.kpiType === 'BOOLEAN' ? val.booleanValue : null,
        recordedDate: selectedDate.value,
        note: val.note || null
      })
    }))
    ElMessage.success(`${targets.length}개 KPI 실적을 저장했습니다.`)
    await loadRecords()
  } finally {
    savingAll.value = false
  }
}

// ── REC-3: 주/월 단위 날짜 이동 ──
function changeWeek(delta) {
  const next = dayjs(selectedDate.value).add(delta * 7, 'day').format('YYYY-MM-DD')
  selectedDate.value = next
  loadRecords()
}

function changeMonth(delta) {
  const next = dayjs(selectedDate.value).add(delta, 'month').format('YYYY-MM-DD')
  selectedDate.value = next
  loadRecords()
}

function goToday() {
  selectedDate.value = dayjs().format('YYYY-MM-DD')
  loadRecords()
}

// ── REC-4: 메모 자동완성 ──
const noteSuggestionsCache = reactive({}) // kpiId → string[]
const noteSuggestionsLoading = reactive({})

async function loadNoteSuggestions(kpiId) {
  if (noteSuggestionsCache[kpiId] || noteSuggestionsLoading[kpiId]) return
  noteSuggestionsLoading[kpiId] = true
  try {
    const end = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(180, 'day').format('YYYY-MM-DD')
    const res = await recordApi.getByKpiId(kpiId, { startDate: start, endDate: end })
    const notes = (res.data || [])
      .filter(r => r.note?.trim())
      .map(r => r.note.trim())
    noteSuggestionsCache[kpiId] = [...new Set(notes)] // 중복 제거
  } catch {
    noteSuggestionsCache[kpiId] = []
  } finally {
    noteSuggestionsLoading[kpiId] = false
  }
}

function getNoteAutocomplete(kpiId, query, cb) {
  const suggestions = noteSuggestionsCache[kpiId] || []
  const filtered = query
    ? suggestions.filter(n => n.toLowerCase().includes(query.toLowerCase()))
    : suggestions
  cb(filtered.map(v => ({ value: v })))
}

// ── KPI 다이얼로그 ──
function openKpiAddDialog() {
  editingKpi.value = null
  store.fetchCategories()
  kpiDialogVisible.value = true
}

function openKpiEditDialog(kpi) {
  editingKpi.value = { ...kpi, categoryId: kpi.categoryId }
  store.fetchCategories()
  kpiDialogVisible.value = true
}

async function onKpiFormSubmit(data) {
  if (editingKpi.value) {
    await store.updateKpi(editingKpi.value.id, data)
  } else {
    await store.createKpi(data)
  }
  kpiDialogVisible.value = false
  await loadRecords()
}

const progressColor  = (r) => r >= 80 ? '#10B981' : r >= 50 ? '#F59E0B' : '#EF4444'
const frequencyLabel = (f) => ({ DAILY: '매일', WEEKLY: '매주', MONTHLY: '매월' }[f] ?? f)

onMounted(async () => {
  await Promise.all([loadRecords(), store.fetchDashboard()])
})
</script>

<style scoped>
.page-container { padding: 24px 28px; }

/* ── 날짜 헤더 ── */
.date-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  background: var(--bg-card);
  padding: 16px 20px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xs);
  flex-wrap: wrap;
}

.date-nav-block { display: flex; flex-direction: column; gap: 6px; }
.date-selector { display: flex; align-items: center; gap: 6px; }

.nav-jump-btn {
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
  min-width: unset !important;
}

.today-btn { font-size: 0.78rem; height: 28px; padding: 0 10px; }

.date-label {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-project);
  background: rgba(48,127,226,0.08);
  padding: 3px 10px;
  border-radius: 99px;
}

/* 날짜 컨텍스트 */
.date-context {
  display: flex;
  gap: 8px;
  padding-left: 2px;
}

.date-context-week {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.date-context-month {
  font-size: 0.75rem;
  color: var(--text-muted);
  padding: 2px 4px;
}

/* ── 요약 영역 ── */
.date-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.date-summary-actions { display: flex; gap: 8px; }

.summary-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-success);
  background: rgba(16,185,129,0.08);
  padding: 6px 12px;
  border-radius: 99px;
  transition: all var(--transition-base);
}

.summary-pill.all-done {
  background: rgba(16,185,129,0.14);
  color: #0D9E6A;
}

/* ── 미완료 필터 버튼 ── */
.due-filter-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-hover);
  border: 1.5px solid var(--border-color);
  padding: 5px 12px;
  border-radius: 99px;
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;
}

.due-filter-btn:hover {
  background: rgba(48,127,226,0.06);
  border-color: rgba(48,127,226,0.4);
  color: var(--color-project);
}

.due-filter-btn.active {
  background: rgba(48,127,226,0.08);
  border-color: var(--color-project);
  color: var(--color-project);
  box-shadow: 0 0 0 3px rgba(48,127,226,0.10);
}

.due-badge {
  background: var(--color-danger);
  color: white;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 99px;
  line-height: 1.4;
}

/* ── 전체 완료 상태 ── */
.all-clear-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 0;
  gap: 8px;
}

.all-clear-icon { font-size: 3rem; }
.all-clear-title { font-size: 1.2rem; font-weight: 800; color: var(--color-success); }
.all-clear-sub { font-size: 0.85rem; color: var(--text-secondary); }

/* ── 섹션 ── */
.record-sections { display: flex; flex-direction: column; gap: 20px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.cat-title { display: flex; align-items: center; gap: 8px; }
.category-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.category-name { font-size: 1rem; font-weight: 700; color: var(--text-primary); }
.cat-count {
  font-size: 0.75rem; color: var(--text-muted);
  background: var(--bg-hover); padding: 2px 8px; border-radius: 99px;
}

.cat-header-right { display: flex; align-items: center; gap: 10px; }
.cat-done { font-size: 0.78rem; color: var(--color-success); font-weight: 600; }
.collapse-btn {
  width: 24px; height: 24px; border: 1px solid var(--border-color); border-radius: var(--radius-sm);
  background: var(--bg-card); cursor: pointer; display: flex; align-items: center;
  justify-content: center; color: var(--text-muted); transition: all var(--transition-fast);
}
.collapse-btn:hover { background: var(--bg-hover); color: var(--text-secondary); }

/* ── 카드 그리드 ── */
.record-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.record-card {
  background: var(--bg-card);
  border: 1.5px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 16px;
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.record-card:hover { box-shadow: var(--shadow-sm); }

.record-card.recorded {
  border-color: rgba(16,185,129,0.4);
  background: linear-gradient(180deg, rgba(16,185,129,0.04) 0%, var(--bg-card) 100%);
}

.record-card.period-done {
  border-color: rgba(48,127,226,0.35);
  background: linear-gradient(180deg, rgba(48,127,226,0.04) 0%, var(--bg-card) 100%);
}

/* ── 카드 상단 ── */
.record-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}

.record-kpi-name { font-weight: 700; font-size: 0.95rem; color: var(--text-primary); }
.record-kpi-meta { font-size: 0.75rem; color: var(--text-muted); margin-top: 3px; }
.card-top-actions { display: flex; align-items: center; gap: 6px; }
.done-tag { flex-shrink: 0; }
.period-done-tag { flex-shrink: 0; font-size: 0.68rem !important; }

.icon-btn {
  width: 26px; height: 26px;
  border: 1px solid var(--border-color); border-radius: var(--radius-sm);
  background: var(--bg-card); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); transition: all var(--transition-fast);
}
.icon-btn:hover { background: var(--bg-hover); color: var(--color-project); border-color: rgba(48,127,226,0.35); }

/* ── 주기 완료 배너 ── */
.period-done-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--color-project);
  background: rgba(48,127,226,0.06);
  border: 1px solid rgba(48,127,226,0.2);
  border-radius: var(--radius-md);
  padding: 6px 10px;
  margin-bottom: 10px;
}

/* ── 저장값 표시 ── */
.saved-value-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.78rem;
  background: rgba(16,185,129,0.06);
  border: 1px solid rgba(16,185,129,0.22);
  border-radius: var(--radius-md);
  padding: 6px 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.saved-label { color: var(--color-success); font-weight: 600; }
.saved-value { color: #0D9E6A; font-weight: 700; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.saved-note  { color: var(--text-secondary); font-style: italic; }

/* ── 입력 영역 ── */
.record-input-area { margin-bottom: 10px; }
.input-with-unit { display: flex; align-items: center; gap: 8px; }
.unit-label { font-size: 0.85rem; color: var(--text-secondary); white-space: nowrap; }

.rate-preview {
  display: flex; align-items: center; gap: 8px;
  margin-top: 8px;
}
.rate-text { font-size: 0.75rem; font-weight: 600; color: var(--text-secondary); white-space: nowrap; }

.record-note-area { margin-bottom: 12px; }

/* ── 액션 버튼 ── */
.record-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--border-color);
}

/* ── 빈 상태 ── */
.empty-state {
  display: flex; flex-direction: column; align-items: center; padding: 80px 0;
}

.mt-3 { margin-top: 12px; }
</style>
