<template>
  <div class="page-container">

    <!-- 페이지 헤더 -->
    <div class="star-page-header">
      <div class="star-header-info">
        <div class="star-header-title">
          <Star :size="20" class="star-title-icon" />
          STAR 노트
        </div>
        <p class="star-header-desc">
          AWS 리더십 원칙 기반 경험 스토리 &middot; Situation → Task → Action → Result
        </p>
      </div>
      <button class="create-btn" @click="openCreate">
        <Plus :size="15" /> 새 스토리 작성
      </button>
    </div>

    <!-- 통계 요약 (스토리가 있을 때만) -->
    <div class="star-stats" v-if="stories.length">
      <div class="star-stat-item">
        <div class="star-stat-val">{{ stories.length }}</div>
        <div class="star-stat-label">총 스토리</div>
      </div>
      <div class="star-stat-item">
        <div class="star-stat-val">{{ coveredLpCount }}</div>
        <div class="star-stat-label">커버된 LP</div>
      </div>
      <div class="star-stat-item">
        <div class="star-stat-val">{{ storiesWithKpi }}</div>
        <div class="star-stat-label">KPI 연결</div>
      </div>
      <div class="star-stat-item">
        <div class="star-stat-val">{{ completedStories }}</div>
        <div class="star-stat-label">S/T/A/R 완성</div>
      </div>
    </div>

    <!-- LP 커버리지 게이지 -->
    <div class="lp-coverage" v-if="stories.length">
      <div class="lp-coverage-header">
        <span class="lp-coverage-label">리더십 원칙 커버리지</span>
        <span class="lp-coverage-val">{{ coveredLpCount }} / 14</span>
      </div>
      <div class="lp-chips-row">
        <div
          v-for="lp in LP_OPTIONS"
          :key="lp.value"
          class="lp-coverage-chip"
          :class="{ covered: coveredLpSet.has(lp.value) }"
          :style="coveredLpSet.has(lp.value) ? { background: lp.color + '22', borderColor: lp.color, color: lp.color } : {}"
          :title="lp.label + ' · ' + lp.ko"
        >{{ lp.shortLabel }}</div>
      </div>
    </div>

    <!-- 검색 + 필터 -->
    <div class="filter-bar">
      <div class="filter-search">
        <Search :size="14" class="filter-search-icon" />
        <input
          v-model="searchQ"
          class="filter-search-input"
          placeholder="스토리 제목, 내용 검색..."
        />
        <button v-if="searchQ" class="filter-clear-btn" @click="searchQ = ''">
          <X :size="12" />
        </button>
      </div>
      <div class="filter-lp-scroll">
        <button
          class="lp-filter-btn"
          :class="{ active: filterLp === '' }"
          @click="filterLp = ''"
        >전체 ({{ stories.length }})</button>
        <button
          v-for="lp in usedLps"
          :key="lp.value"
          class="lp-filter-btn"
          :class="{ active: filterLp === lp.value }"
          :style="filterLp === lp.value ? { background: lp.color + '22', borderColor: lp.color, color: lp.color } : {}"
          @click="filterLp = lp.value"
        >{{ lp.shortLabel }} ({{ lpCount(lp.value) }})</button>
      </div>
    </div>

    <!-- 스토리 그리드 -->
    <div class="star-grid" v-if="filteredStories.length">
      <div
        v-for="story in filteredStories"
        :key="story.id"
        class="star-card"
        @click="openEdit(story)"
      >
        <div class="star-card-top">
          <div class="star-card-meta">
            <span
              v-if="story.lpTag"
              class="lp-chip"
              :style="{ background: lpColor(story.lpTag) + '22', borderColor: lpColor(story.lpTag), color: lpColor(story.lpTag) }"
            >{{ lpShortLabel(story.lpTag) }}</span>
            <span v-if="story.kpiId" class="kpi-link-chip">
              <Link2 :size="10" /> {{ kpiName(story.kpiId) }}
            </span>
          </div>
          <span class="star-card-date">{{ story.updatedAt }}</span>
        </div>

        <div class="star-card-title">{{ story.title || '(제목 없음)' }}</div>

        <!-- S/T/A/R 미리보기 -->
        <div class="star-card-preview">
          <div v-if="story.situation" class="preview-row">
            <span class="preview-letter s-letter">S</span>
            <span class="preview-text">{{ truncate(story.situation, 60) }}</span>
          </div>
          <div v-if="story.result" class="preview-row">
            <span class="preview-letter r-letter">R</span>
            <span class="preview-text result-text">{{ truncate(story.result, 60) }}</span>
          </div>
        </div>

        <!-- 완성도 바 -->
        <div class="completeness-row">
          <span
            v-for="f in ['situation','task','action','result']"
            :key="f"
            class="complete-dot"
            :class="{ filled: story[f]?.trim() }"
            :title="starFieldLabel(f)"
          ></span>
          <span class="complete-label">
            {{ [story.situation, story.task, story.action, story.result].filter(v => v?.trim()).length }}/4 완성
          </span>
        </div>

        <div class="star-card-footer">
          <button class="card-edit-hint">클릭하여 편집</button>
          <button class="card-delete-btn" @click.stop="confirmDelete(story)">
            <Trash2 :size="13" />
          </button>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="star-empty">
      <div class="star-empty-icon">⭐</div>
      <div class="star-empty-title">
        {{ searchQ || filterLp ? '검색 결과가 없습니다' : 'STAR 스토리를 작성해보세요' }}
      </div>
      <p class="star-empty-desc" v-if="!searchQ && !filterLp">
        AWS 면접의 핵심인 STAR 방식으로 경험을 정리하면<br/>
        리더십 원칙 질문에 자신있게 답할 수 있습니다.
      </p>
      <button v-if="!searchQ && !filterLp" class="create-btn" style="margin-top:16px" @click="openCreate">
        <Plus :size="15" /> 첫 스토리 작성하기
      </button>
    </div>

    <!-- 생성/편집 모달 -->
    <Transition name="modal-fade">
      <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
        <div class="star-modal">
          <div class="modal-header">
            <div class="modal-title">
              <Star :size="16" class="modal-title-icon" />
              {{ editingId ? 'STAR 스토리 편집' : '새 STAR 스토리' }}
            </div>
            <button class="modal-close-btn" @click="closeModal"><X :size="16" /></button>
          </div>

          <div class="modal-body">
            <!-- 제목 -->
            <div class="form-group">
              <label class="form-label">스토리 제목 <span class="required">*</span></label>
              <input
                v-model="form.title"
                class="form-input"
                placeholder="예: API 레이턴시 50% 개선, 팀 온보딩 프로세스 구축..."
                maxlength="100"
                autofocus
              />
            </div>

            <!-- LP + KPI 행 -->
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label">리더십 원칙 (LP)</label>
                <select v-model="form.lpTag" class="form-select">
                  <option value="">선택 안 함</option>
                  <option v-for="lp in LP_OPTIONS" :key="lp.value" :value="lp.value">
                    {{ lp.label }} · {{ lp.ko }}
                  </option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">연결 KPI <span class="optional">(선택)</span></label>
                <select v-model="form.kpiId" class="form-select">
                  <option value="">연결 안 함</option>
                  <option v-for="kpi in store.kpis" :key="kpi.id" :value="kpi.id">
                    {{ kpi.name }}
                  </option>
                </select>
              </div>
            </div>

            <!-- S / T / A / R 필드 -->
            <div class="star-fields">
              <div
                v-for="f in STAR_FIELDS"
                :key="f.key"
                class="star-field"
                :class="{ filled: form[f.key]?.trim() }"
              >
                <div class="star-field-header">
                  <span class="star-field-letter" :style="{ background: f.color }">{{ f.letter }}</span>
                  <div class="star-field-label-group">
                    <span class="star-field-name">{{ f.name }}</span>
                    <span class="star-field-desc">{{ f.desc }}</span>
                  </div>
                </div>
                <textarea
                  v-model="form[f.key]"
                  class="star-textarea"
                  :placeholder="f.placeholder"
                  rows="4"
                ></textarea>
                <div class="star-field-tip">
                  <Lightbulb :size="11" class="tip-icon" />
                  {{ f.tip }}
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-cancel-btn" @click="closeModal">취소</button>
            <button
              class="modal-save-btn"
              @click="saveStory"
              :disabled="!form.title.trim()"
            >
              <Save :size="14" /> 저장
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 삭제 확인 모달 -->
    <Transition name="modal-fade">
      <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
        <div class="confirm-modal">
          <div class="confirm-icon">🗑️</div>
          <div class="confirm-title">스토리를 삭제할까요?</div>
          <div class="confirm-desc">「{{ deleteTarget?.title || '(제목 없음)' }}」을 삭제하면 복구할 수 없습니다.</div>
          <div class="confirm-actions">
            <button class="modal-cancel-btn" @click="deleteTarget = null">취소</button>
            <button class="confirm-delete-btn" @click="doDelete">삭제</button>
          </div>
        </div>
      </div>
    </Transition>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useKpiStore } from '@/store/kpiStore'
import { Star, Plus, Search, X, Trash2, Link2, Save, Lightbulb } from 'lucide-vue-next'
import dayjs from 'dayjs'

const store = useKpiStore()

// ── 리더십 원칙 목록 ──
const LP_OPTIONS = [
  { value: 'customer_obsession',  label: 'Customer Obsession',              ko: '고객 집착',     shortLabel: 'CO',  color: '#3B82F6' },
  { value: 'ownership',           label: 'Ownership',                       ko: '주인의식',      shortLabel: 'OW',  color: '#10B981' },
  { value: 'invent_simplify',     label: 'Invent and Simplify',             ko: '발명과 단순화',  shortLabel: 'IS',  color: '#8B5CF6' },
  { value: 'are_right',           label: 'Are Right, A Lot',                ko: '높은 판단력',   shortLabel: 'AR',  color: '#F59E0B' },
  { value: 'learn_curious',       label: 'Learn and Be Curious',            ko: '학습과 호기심',  shortLabel: 'LC',  color: '#06B6D4' },
  { value: 'hire_develop',        label: 'Hire and Develop the Best',       ko: '최고 인재',     shortLabel: 'HD',  color: '#EC4899' },
  { value: 'highest_standards',   label: 'Insist on the Highest Standards', ko: '최고 기준',     shortLabel: 'HS',  color: '#EF4444' },
  { value: 'think_big',           label: 'Think Big',                       ko: '큰 그림',      shortLabel: 'TB',  color: '#6366F1' },
  { value: 'bias_action',         label: 'Bias for Action',                 ko: '행동 편향',     shortLabel: 'BA',  color: '#F97316' },
  { value: 'frugality',           label: 'Frugality',                       ko: '절약',         shortLabel: 'FR',  color: '#84CC16' },
  { value: 'earn_trust',          label: 'Earn Trust',                      ko: '신뢰 구축',     shortLabel: 'ET',  color: '#14B8A6' },
  { value: 'dive_deep',           label: 'Dive Deep',                       ko: '깊이 파고들기',  shortLabel: 'DD',  color: '#A855F7' },
  { value: 'backbone',            label: 'Have Backbone; Disagree & Commit',ko: '원칙 고수',     shortLabel: 'BB',  color: '#DC2626' },
  { value: 'deliver_results',     label: 'Deliver Results',                 ko: '성과 달성',     shortLabel: 'DR',  color: '#059669' },
]

// ── STAR 필드 정의 ──
const STAR_FIELDS = [
  {
    key: 'situation',
    letter: 'S',
    name: 'Situation (상황)',
    desc: '배경과 맥락',
    color: '#3B82F6',
    placeholder: '언제, 어디서, 어떤 상황이었는지 설명하세요.\n예: 2024년 Q1, 사용자 API 레이턴시가 급증하여 SLA 99.9%가 위협받고 있었습니다.',
    tip: '간략하게 — 2~3문장으로 맥락을 설명하세요.'
  },
  {
    key: 'task',
    letter: 'T',
    name: 'Task (과제)',
    desc: '나의 역할과 목표',
    color: '#F59E0B',
    placeholder: '해결해야 했던 문제나 달성하고자 했던 목표를 기술하세요.\n예: API 응답시간을 200ms 이하로 줄여 SLA를 회복하고 사용자 경험을 개선해야 했습니다.',
    tip: '"나"의 책임과 목표를 명확히 기술하세요.'
  },
  {
    key: 'action',
    letter: 'A',
    name: 'Action (행동)',
    desc: '내가 직접 한 것',
    color: '#10B981',
    placeholder: '문제를 해결하기 위해 본인이 직접 취한 구체적인 행동과 단계를 기술하세요.\n예: 먼저 CloudWatch로 병목을 파악했고, ElastiCache 캐시 계층을 새로 설계했으며, 비동기 처리로 전환했습니다.',
    tip: '"우리"가 아닌 "나"의 행동에 초점을 맞추세요. 단계별로 기술하면 좋습니다.'
  },
  {
    key: 'result',
    letter: 'R',
    name: 'Result (결과)',
    desc: '수치화된 성과',
    color: '#EC4899',
    placeholder: '행동으로 얻은 구체적이고 수치화된 성과와 배운 점을 기술하세요.\n예: API 레이턴시 52% 감소 (380ms → 182ms), SLA 99.9% 회복, 고객 불만 티켓 80% 감소.',
    tip: '숫자로 말하세요 — %, 배수, 절대치, 비용 절감액을 활용하세요.'
  },
]

// ── localStorage ──
const STORAGE_KEY = 'workoop-star-stories'

function loadStories() {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [] } catch { return [] }
}
function saveStories(arr) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(arr))
}

const stories = ref(loadStories())

// ── 필터 ──
const searchQ  = ref('')
const filterLp = ref('')

const filteredStories = computed(() => {
  let list = stories.value
  if (filterLp.value) list = list.filter(s => s.lpTag === filterLp.value)
  if (searchQ.value.trim()) {
    const q = searchQ.value.toLowerCase()
    list = list.filter(s =>
      [s.title, s.situation, s.task, s.action, s.result].some(v => v?.toLowerCase().includes(q))
    )
  }
  return [...list].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
})

// ── 통계 ──
const coveredLpSet    = computed(() => new Set(stories.value.map(s => s.lpTag).filter(Boolean)))
const coveredLpCount  = computed(() => coveredLpSet.value.size)
const storiesWithKpi  = computed(() => stories.value.filter(s => s.kpiId).length)
const completedStories = computed(() =>
  stories.value.filter(s => s.situation?.trim() && s.task?.trim() && s.action?.trim() && s.result?.trim()).length
)

// 사용된 LP만 필터 버튼에 표시
const usedLps = computed(() =>
  LP_OPTIONS.filter(lp => stories.value.some(s => s.lpTag === lp.value))
)
const lpCount = (val) => stories.value.filter(s => s.lpTag === val).length

// ── 헬퍼 ──
function lpColor(val) {
  return LP_OPTIONS.find(l => l.value === val)?.color || '#94A3B8'
}
function lpShortLabel(val) {
  const lp = LP_OPTIONS.find(l => l.value === val)
  return lp ? `${lp.shortLabel} · ${lp.ko}` : ''
}
function kpiName(id) {
  return store.kpis?.find(k => k.id === id)?.name || id
}
function truncate(str, len = 70) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '…' : str
}
function starFieldLabel(key) {
  return { situation: 'Situation', task: 'Task', action: 'Action', result: 'Result' }[key] || key
}

// ── 모달 폼 ──
const modalOpen  = ref(false)
const editingId  = ref(null)
const form = ref({ title: '', lpTag: '', kpiId: '', situation: '', task: '', action: '', result: '' })

function openCreate() {
  editingId.value = null
  form.value = { title: '', lpTag: '', kpiId: '', situation: '', task: '', action: '', result: '' }
  modalOpen.value = true
}
function openEdit(story) {
  editingId.value = story.id
  form.value = { ...story }
  modalOpen.value = true
}
function closeModal() {
  modalOpen.value = false
}

function saveStory() {
  if (!form.value.title.trim()) return
  const now = dayjs().format('YYYY-MM-DD')

  if (editingId.value) {
    stories.value = stories.value.map(s =>
      s.id === editingId.value ? { ...form.value, id: s.id, createdAt: s.createdAt, updatedAt: now } : s
    )
  } else {
    stories.value = [
      ...stories.value,
      { ...form.value, id: Date.now().toString(), createdAt: now, updatedAt: now }
    ]
  }
  saveStories(stories.value)
  closeModal()
}

// ── 삭제 ──
const deleteTarget = ref(null)
function confirmDelete(story) { deleteTarget.value = story }
function doDelete() {
  stories.value = stories.value.filter(s => s.id !== deleteTarget.value.id)
  saveStories(stories.value)
  deleteTarget.value = null
}
</script>

<style scoped>
.page-container { padding: 24px 28px; max-width: 1200px; }

/* ── 페이지 헤더 ── */
.star-page-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 20px; gap: 16px;
}
.star-header-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 1.25rem; font-weight: 800; color: var(--text-primary);
}
.star-title-icon { color: var(--color-warning); }
.star-header-desc { font-size: 0.8rem; color: var(--text-secondary); margin-top: 4px; }

.create-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px; border: none; border-radius: var(--radius-md);
  background: var(--text-primary); color: var(--bg-main); cursor: pointer;
  font-size: 0.84rem; font-weight: 700; white-space: nowrap;
  transition: opacity var(--transition-fast); flex-shrink: 0;
}
.create-btn:hover { opacity: 0.85; }

/* ── 통계 ── */
.star-stats {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 12px; margin-bottom: 16px;
}
.star-stat-item {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 14px 18px;
  text-align: center; box-shadow: var(--shadow-xs);
}
.star-stat-val   { font-size: 1.6rem; font-weight: 800; color: var(--text-primary); line-height: 1; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.star-stat-label { font-size: 0.72rem; color: var(--text-secondary); margin-top: 5px; }

/* ── LP 커버리지 ── */
.lp-coverage {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 14px 18px; margin-bottom: 16px; box-shadow: var(--shadow-xs);
}
.lp-coverage-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.lp-coverage-label { font-size: 0.8rem; font-weight: 700; color: var(--text-primary); }
.lp-coverage-val   { font-size: 0.8rem; font-weight: 700; color: var(--color-project); }
.lp-chips-row { display: flex; flex-wrap: wrap; gap: 6px; }
.lp-coverage-chip {
  font-size: 0.68rem; font-weight: 700;
  padding: 3px 9px; border-radius: 99px;
  border: 1px solid var(--border-color); color: var(--text-muted);
  background: var(--bg-hover); transition: all var(--transition-base); cursor: default;
}
.lp-coverage-chip.covered { font-weight: 800; }

/* ── 필터 바 ── */
.filter-bar {
  display: flex; flex-direction: column; gap: 10px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 12px 14px;
  margin-bottom: 16px; box-shadow: var(--shadow-xs);
}
.filter-search {
  display: flex; align-items: center; gap: 8px;
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 7px 12px;
}
.filter-search-icon { color: var(--text-muted); flex-shrink: 0; }
.filter-search-input {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 0.85rem; color: var(--text-primary); font-family: inherit;
}
.filter-search-input::placeholder { color: var(--text-muted); }
.filter-clear-btn {
  background: none; border: none; cursor: pointer;
  color: var(--text-muted); display: flex; align-items: center; padding: 0;
  transition: color var(--transition-fast);
}
.filter-clear-btn:hover { color: var(--text-secondary); }

.filter-lp-scroll {
  display: flex; flex-wrap: wrap; gap: 6px;
}
.lp-filter-btn {
  padding: 4px 12px; border: 1px solid var(--border-color);
  border-radius: 99px; background: var(--bg-card);
  font-size: 0.72rem; font-weight: 600; color: var(--text-secondary);
  cursor: pointer; transition: all var(--transition-fast); white-space: nowrap;
}
.lp-filter-btn:hover { border-color: var(--gray-300); color: var(--text-primary); }
.lp-filter-btn.active { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-main); }

/* ── 스토리 그리드 ── */
.star-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.star-card {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 16px; box-shadow: var(--shadow-xs);
  cursor: pointer; transition: all var(--transition-base);
  display: flex; flex-direction: column; gap: 10px;
}
.star-card:hover {
  border-color: var(--gray-300);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.star-card-top {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
}
.star-card-meta { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.lp-chip {
  font-size: 0.65rem; font-weight: 700;
  padding: 3px 9px; border-radius: 99px; border: 1px solid;
  white-space: nowrap;
}
.kpi-link-chip {
  display: flex; align-items: center; gap: 4px;
  font-size: 0.65rem; color: var(--text-secondary);
  background: var(--bg-hover); border: 1px solid var(--border-color);
  padding: 2px 8px; border-radius: 99px;
  white-space: nowrap; overflow: hidden; max-width: 100px;
  text-overflow: ellipsis;
}
.star-card-date { font-size: 0.68rem; color: var(--text-muted); flex-shrink: 0; }

.star-card-title {
  font-size: 0.95rem; font-weight: 700; color: var(--text-primary);
  line-height: 1.35;
}

/* 미리보기 */
.star-card-preview { display: flex; flex-direction: column; gap: 5px; }
.preview-row { display: flex; align-items: flex-start; gap: 8px; }
.preview-letter {
  width: 18px; height: 18px; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.65rem; font-weight: 800; color: white;
  flex-shrink: 0; margin-top: 1px;
}
.s-letter { background: var(--color-project); }
.r-letter { background: #EC4899; }
.preview-text {
  font-size: 0.75rem; color: var(--text-secondary); line-height: 1.4;
}
.result-text { color: var(--text-primary); font-weight: 500; }

/* 완성도 바 */
.completeness-row {
  display: flex; align-items: center; gap: 5px;
}
.complete-dot {
  width: 22px; height: 5px; border-radius: 3px;
  background: var(--border-color); transition: background var(--transition-base);
}
.complete-dot.filled { background: var(--color-success); }
.complete-label { font-size: 0.65rem; color: var(--text-muted); margin-left: 4px; }

.star-card-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 8px; border-top: 1px solid var(--border-color);
}
.card-edit-hint {
  font-size: 0.68rem; color: var(--text-muted); background: none; border: none; cursor: pointer;
  padding: 0;
}
.card-delete-btn {
  width: 26px; height: 26px; border-radius: var(--radius-sm);
  border: 1px solid var(--border-color); background: var(--bg-card);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); cursor: pointer; transition: all var(--transition-fast);
}
.card-delete-btn:hover { border-color: rgba(239,68,68,0.4); color: var(--color-danger); background: rgba(239,68,68,0.06); }

/* ── 빈 상태 ── */
.star-empty {
  text-align: center; padding: 60px 20px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-xl); box-shadow: var(--shadow-xs);
}
.star-empty-icon  { font-size: 3rem; margin-bottom: 12px; }
.star-empty-title { font-size: 1rem; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.star-empty-desc  { font-size: 0.82rem; color: var(--text-muted); line-height: 1.6; }

/* ── 모달 공통 ── */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(17,24,39,0.50);
  display: flex; align-items: center; justify-content: center;
  z-index: 9000; padding: 20px;
}
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity var(--transition-base); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }

/* ── STAR 편집 모달 ── */
.star-modal {
  background: var(--bg-card); border-radius: var(--radius-xl);
  width: 740px; max-width: 100%;
  max-height: 90vh; display: flex; flex-direction: column;
  box-shadow: var(--shadow-xl);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 22px 14px; border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}
.modal-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 0.95rem; font-weight: 700; color: var(--text-primary);
}
.modal-title-icon { color: var(--color-warning); }
.modal-close-btn {
  width: 30px; height: 30px; border: none; background: var(--bg-hover);
  border-radius: var(--radius-md); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary); transition: all var(--transition-fast);
}
.modal-close-btn:hover { background: var(--border-color); }

.modal-body {
  padding: 20px 22px; overflow-y: auto; flex: 1;
  display: flex; flex-direction: column; gap: 16px;
}

/* 폼 요소 */
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 0.75rem; font-weight: 700; color: var(--text-secondary); }
.required { color: var(--color-danger); }
.optional { font-weight: 400; color: var(--text-muted); }
.form-input, .form-select {
  padding: 9px 12px; border: 1px solid var(--border-color);
  border-radius: var(--radius-md); font-size: 0.85rem; color: var(--text-primary);
  outline: none; font-family: inherit; transition: border-color var(--transition-fast);
  background: var(--bg-card);
}
.form-input:focus, .form-select:focus { border-color: var(--color-project); }
.form-row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

/* S/T/A/R 필드들 */
.star-fields { display: flex; flex-direction: column; gap: 14px; }
.star-field {
  border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 14px;
  transition: border-color var(--transition-base);
}
.star-field.filled { border-color: rgba(16,185,129,0.35); background: rgba(16,185,129,0.03); }
.star-field-header {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
}
.star-field-letter {
  width: 28px; height: 28px; border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; font-weight: 900; color: white; flex-shrink: 0;
}
.star-field-label-group { display: flex; flex-direction: column; gap: 1px; }
.star-field-name { font-size: 0.85rem; font-weight: 700; color: var(--text-primary); }
.star-field-desc { font-size: 0.68rem; color: var(--text-muted); }

.star-textarea {
  width: 100%; padding: 10px 12px;
  border: 1px solid var(--border-color); border-radius: var(--radius-md);
  font-size: 0.82rem; color: var(--text-primary); line-height: 1.6;
  outline: none; resize: vertical; font-family: inherit;
  box-sizing: border-box; transition: border-color var(--transition-fast);
  background: var(--bg-card); min-height: 80px;
}
.star-textarea:focus { border-color: var(--color-project); }
.star-textarea::placeholder { color: var(--text-muted); }

.star-field-tip {
  display: flex; align-items: center; gap: 5px;
  margin-top: 7px;
  font-size: 0.68rem; color: var(--text-muted);
}
.tip-icon { color: var(--color-warning); flex-shrink: 0; }

/* 모달 푸터 */
.modal-footer {
  display: flex; gap: 8px; justify-content: flex-end;
  padding: 14px 22px; border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}
.modal-cancel-btn {
  padding: 8px 18px; border: 1px solid var(--border-color); border-radius: var(--radius-md);
  background: var(--bg-card); color: var(--text-secondary); cursor: pointer;
  font-size: 0.83rem; font-weight: 600; transition: all var(--transition-fast);
}
.modal-cancel-btn:hover { background: var(--bg-hover); }
.modal-save-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 20px; border: none; border-radius: var(--radius-md);
  background: var(--text-primary); color: var(--bg-main); cursor: pointer;
  font-size: 0.83rem; font-weight: 700; transition: opacity var(--transition-fast);
}
.modal-save-btn:hover:not(:disabled) { opacity: 0.85; }
.modal-save-btn:disabled { opacity: 0.4; cursor: default; }

/* ── 삭제 확인 모달 ── */
.confirm-modal {
  background: var(--bg-card); border-radius: var(--radius-xl);
  width: 360px; padding: 28px 24px;
  text-align: center;
  box-shadow: var(--shadow-xl);
}
.confirm-icon  { font-size: 2.5rem; margin-bottom: 12px; }
.confirm-title { font-size: 1rem; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.confirm-desc  { font-size: 0.82rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 20px; }
.confirm-actions { display: flex; gap: 8px; justify-content: center; }
.confirm-delete-btn {
  padding: 8px 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-danger); color: white; cursor: pointer;
  font-size: 0.83rem; font-weight: 700; transition: opacity var(--transition-fast);
}
.confirm-delete-btn:hover { opacity: 0.88; }
</style>
