<template>
  <div class="page-container">

    <!-- 탭 헤더 -->
    <div class="review-tabs">
      <button class="rtab" :class="{ active: activeTab === 'weekly' }" @click="activeTab = 'weekly'">
        <CalendarDays :size="14" /> 주간 회고
      </button>
      <button class="rtab" :class="{ active: activeTab === 'monthly' }" @click="activeTab = 'monthly'">
        <CalendarCheck :size="14" /> 월간 회고
      </button>
      <button class="rtab" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
        <ClipboardList :size="14" /> 회고 기록
      </button>
    </div>

    <!-- ── 주간 회고 ── -->
    <template v-if="activeTab === 'weekly'">

      <!-- 에너지 추이 차트 -->
      <div class="energy-history-card">
        <div class="ehc-title">
          🔋 최근 14일 에너지 추이
          <span class="ehc-hint">데일리 스크럼에서 기록된 에너지 레벨</span>
        </div>
        <div class="ehc-bars">
          <div v-for="d in energyHistory" :key="d.date" class="ehc-day">
            <div class="ehc-bar-wrap">
              <div
                class="ehc-bar-fill"
                :style="{ height: (d.energy / 5 * 100) + '%', background: energyColor(d.energy) }"
                :title="`${d.label}: 에너지 ${d.energy}/5`"
              ></div>
            </div>
            <div class="ehc-label" :class="{ today: d.isToday }">{{ d.dayLabel }}</div>
            <div class="ehc-val" :style="{ color: energyColor(d.energy) }">
              {{ d.energy > 0 ? d.energy : '-' }}
            </div>
          </div>
        </div>
        <div class="ehc-legend">
          <span class="ehc-dot" style="background:#10B981"></span> 높음 (4-5)
          <span class="ehc-dot" style="background:#F59E0B; margin-left:12px"></span> 보통 (3)
          <span class="ehc-dot" style="background:#EF4444; margin-left:12px"></span> 낮음 (1-2)
        </div>
      </div>

      <div class="review-hero">
        <div class="review-hero-icon"><PenLine :size="26" /></div>
        <div>
          <div class="review-hero-title">이번 주 회고</div>
          <div class="review-hero-sub">{{ weekLabel }} · Plans-Progress-Problems 형식</div>
        </div>
        <div class="review-hero-kpi" v-if="store.dashboard">
          <div class="hero-stat">
            <span class="hero-stat-val">{{ store.dashboard.overallAchievementRate }}%</span>
            <span class="hero-stat-label">이번 주 달성률</span>
          </div>
        </div>
      </div>

      <div class="review-form">
        <!-- Plans -->
        <div class="review-section">
          <div class="rs-header">
            <Target :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">Plans — 이번 주 계획한 것</div>
              <div class="rs-hint">이번 주에 달성하려고 했던 목표나 할 일은 무엇이었나요?</div>
            </div>
          </div>
          <el-input
            v-model="weeklyForm.plans"
            type="textarea"
            :rows="3"
            placeholder="예: 월간 매출 목표 80% 달성, 팀 미팅 3회 진행, 교육 이수 4시간..."
          />
        </div>

        <!-- Progress -->
        <div class="review-section">
          <div class="rs-header">
            <CheckSquare :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">Progress — 실제로 진행된 것</div>
              <div class="rs-hint">이번 주에 실제로 완료했거나 진행한 것은 무엇인가요?</div>
            </div>
          </div>
          <el-input
            v-model="weeklyForm.progress"
            type="textarea"
            :rows="3"
            placeholder="예: 매출 72% 달성, 팀 미팅 2회 완료, 신규 고객 3건 확보..."
          />
        </div>

        <!-- Problems -->
        <div class="review-section">
          <div class="rs-header">
            <AlertTriangle :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">Problems — 문제점 및 개선 사항</div>
              <div class="rs-hint">이번 주에 겪은 어려움이나 다음 주에 개선할 점은 무엇인가요?</div>
            </div>
          </div>
          <el-input
            v-model="weeklyForm.problems"
            type="textarea"
            :rows="3"
            placeholder="예: 미팅 준비 시간 부족, 데이터 입력 누락 2일, 집중력 저하..."
          />
        </div>

        <!-- 자유 메모 -->
        <div class="review-section">
          <div class="rs-header">
            <MessageSquare :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">자유 메모</div>
              <div class="rs-hint">추가로 기록하고 싶은 생각이나 다음 주 다짐</div>
            </div>
          </div>
          <el-input
            v-model="weeklyForm.memo"
            type="textarea"
            :rows="2"
            placeholder="자유롭게 기록하세요..."
          />
        </div>

        <!-- 이번 주 점수 -->
        <div class="review-section score-section-form">
          <div class="rs-header">
            <Star :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">이번 주 자기 평가</div>
              <div class="rs-hint">이번 주 자신의 성과를 1~5점으로 평가해주세요</div>
            </div>
          </div>
          <el-rate v-model="weeklyForm.selfScore" :max="5" size="large" />
        </div>

        <div class="review-actions">
          <el-button @click="clearWeeklyForm">초기화</el-button>
          <el-button type="primary" @click="saveWeeklyReview" :loading="saving">
            <Save :size="14" class="mr-1" /> 회고 저장
          </el-button>
        </div>
      </div>
    </template>

    <!-- ── 월간 회고 ── -->
    <template v-if="activeTab === 'monthly'">
      <div class="review-hero">
        <div class="review-hero-icon"><Moon :size="26" /></div>
        <div>
          <div class="review-hero-title">이번 달 회고</div>
          <div class="review-hero-sub">{{ monthLabel }} · 월간 성과 분석 및 다음 달 계획</div>
        </div>
      </div>

      <div class="review-form">
        <div class="review-section">
          <div class="rs-header">
            <Trophy :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">이달의 최고 성과</div>
              <div class="rs-hint">이번 달 가장 뿌듯했던 성과는 무엇인가요?</div>
            </div>
          </div>
          <el-input v-model="monthlyForm.bestAchievement" type="textarea" :rows="2"
            placeholder="예: 분기 목표 초과 달성, 신규 프로젝트 수주..." />
        </div>

        <div class="review-section">
          <div class="rs-header">
            <Heart :size="20" class="rs-icon rs-icon-regret" />
            <div>
              <div class="rs-title">아쉬웠던 점</div>
              <div class="rs-hint">이번 달 아쉽거나 개선이 필요한 부분은?</div>
            </div>
          </div>
          <el-input v-model="monthlyForm.regrets" type="textarea" :rows="2"
            placeholder="예: 교육 이수 목표 달성 실패, 기록 빈도 저하..." />
        </div>

        <div class="review-section">
          <div class="rs-header">
            <Target :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">다음 달 핵심 목표 3가지</div>
              <div class="rs-hint">다음 달 반드시 달성하고 싶은 목표를 구체적으로</div>
            </div>
          </div>
          <div class="goals-list">
            <div v-for="(goal, i) in monthlyForm.nextGoals" :key="i" class="goal-input-row">
              <span class="goal-num">{{ i + 1 }}</span>
              <el-input v-model="monthlyForm.nextGoals[i]" :placeholder="`목표 ${i+1}`" />
            </div>
          </div>
        </div>

        <div class="review-section">
          <div class="rs-header">
            <FileText :size="20" class="rs-icon" />
            <div>
              <div class="rs-title">기타 메모 및 다짐</div>
            </div>
          </div>
          <el-input v-model="monthlyForm.memo" type="textarea" :rows="3"
            placeholder="이번 달을 마무리하는 생각을 자유롭게 기록하세요..." />
        </div>

        <div class="review-section score-section-form">
          <div class="rs-header">
            <Star :size="20" class="rs-icon" />
            <div><div class="rs-title">이번 달 자기 평가</div></div>
          </div>
          <el-rate v-model="monthlyForm.selfScore" :max="5" size="large" />
        </div>

        <div class="review-actions">
          <el-button @click="clearMonthlyForm">초기화</el-button>
          <el-button type="primary" @click="saveMonthlyReview" :loading="saving">
            <Save :size="14" class="mr-1" /> 회고 저장
          </el-button>
        </div>
      </div>
    </template>

    <!-- ── 회고 기록 ── -->
    <template v-if="activeTab === 'history'">
      <div v-if="reviews.length === 0" class="empty-reviews">
        <div class="er-icon"><ClipboardList :size="40" /></div>
        <div class="er-title">저장된 회고가 없습니다</div>
        <div class="er-sub">주간 또는 월간 회고를 작성하고 저장해보세요.</div>
      </div>

      <div v-else class="review-history">
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="ri-header">
            <div class="ri-type-badge" :class="review.type">
              {{ review.type === 'weekly' ? '주간' : '월간' }}
            </div>
            <div class="ri-period">{{ review.period }}</div>
            <div class="ri-score">
              <Star v-for="i in review.selfScore" :key="i" :size="13" fill="#F59E0B" color="#F59E0B" />
            </div>
            <div class="ri-date">{{ review.savedAt }}</div>
            <el-button size="small" text type="danger" @click="deleteReview(review.id)">삭제</el-button>
          </div>

          <!-- 주간 회고 내용 -->
          <div v-if="review.type === 'weekly'" class="ri-body">
            <div v-if="review.plans" class="ri-field"><span class="ri-field-label"><Target :size="11" /> Plans</span><p>{{ review.plans }}</p></div>
            <div v-if="review.progress" class="ri-field"><span class="ri-field-label"><CheckSquare :size="11" /> Progress</span><p>{{ review.progress }}</p></div>
            <div v-if="review.problems" class="ri-field"><span class="ri-field-label"><AlertTriangle :size="11" /> Problems</span><p>{{ review.problems }}</p></div>
            <div v-if="review.memo" class="ri-field"><span class="ri-field-label"><MessageSquare :size="11" /> 메모</span><p>{{ review.memo }}</p></div>
          </div>

          <!-- 월간 회고 내용 -->
          <div v-else class="ri-body">
            <div v-if="review.bestAchievement" class="ri-field"><span class="ri-field-label"><Trophy :size="11" /> 최고 성과</span><p>{{ review.bestAchievement }}</p></div>
            <div v-if="review.regrets" class="ri-field"><span class="ri-field-label"><Heart :size="11" /> 아쉬운 점</span><p>{{ review.regrets }}</p></div>
            <div v-if="review.nextGoals?.length" class="ri-field">
              <span class="ri-field-label"><Target :size="11" /> 다음 달 목표</span>
              <ol class="ri-goals"><li v-for="g in review.nextGoals" :key="g">{{ g }}</li></ol>
            </div>
            <div v-if="review.memo" class="ri-field"><span class="ri-field-label"><FileText :size="11" /> 메모</span><p>{{ review.memo }}</p></div>
          </div>
        </div>
      </div>
    </template>

  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useKpiStore } from '@/store/kpiStore'
import { useBurnoutStore } from '@/store/burnoutStore'
import { scrumApi } from '@/api/scrumApi'
import { reviewApi } from '@/api/reviewApi'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
import {
  CalendarDays, CalendarCheck, ClipboardList,
  PenLine, Moon, Target, CheckSquare, AlertTriangle,
  MessageSquare, Star, Trophy, Heart, FileText, Save
} from 'lucide-vue-next'

dayjs.extend(isoWeek)

const store   = useKpiStore()
const burnout = useBurnoutStore()
const activeTab = ref('weekly')

// 에너지 추이 (14일 — 서버 우선, localStorage 폴백)
const serverEnergyMap = ref({})  // { 'YYYY-MM-DD': energy }

async function loadEnergyFromServer() {
  try {
    const end   = dayjs().format('YYYY-MM-DD')
    const start = dayjs().subtract(13, 'day').format('YYYY-MM-DD')
    const res   = await scrumApi.range(start, end)
    const map   = {}
    for (const s of (res.data || [])) {
      map[s.scrumDate] = s.energy || 0
    }
    serverEnergyMap.value = map
  } catch { /* localStorage 폴백으로 동작 */ }
}

const energyHistory = computed(() =>
  Array.from({ length: 14 }, (_, i) => {
    const d    = dayjs().subtract(13 - i, 'day')
    const date = d.format('YYYY-MM-DD')
    let energy = serverEnergyMap.value[date] ?? 0
    if (!energy) {
      try {
        const ci = JSON.parse(localStorage.getItem(`workoop-checkin-${date}`))
        energy   = ci?.energy || 0
      } catch {}
    }
    return { date, label: d.format('MM/DD'), dayLabel: d.format('dd'), energy, isToday: i === 13 }
  })
)

function energyColor(e) {
  if (e >= 4) return '#10B981'
  if (e === 3) return '#F59E0B'
  if (e >= 1) return '#EF4444'
  return '#E2E8F0'
}
const saving = ref(false)

// 현재 주/월 레이블
const weekLabel = computed(() => {
  const today = dayjs()
  const mon = today.isoWeekday(1).format('M/D')
  const sun = today.isoWeekday(7).format('M/D')
  return `${today.isoWeek()}주차 (${mon}~${sun})`
})

const monthLabel = computed(() => dayjs().format('YYYY년 MM월'))

// 주간 회고 폼
const weeklyForm = reactive({
  plans: '', progress: '', problems: '', memo: '', selfScore: 3
})

// 월간 회고 폼
const monthlyForm = reactive({
  bestAchievement: '', regrets: '',
  nextGoals: ['', '', ''],
  memo: '', selfScore: 3
})

// 회고 기록 (서버)
const reviews = ref([])

async function loadReviews() {
  try {
    const res = await reviewApi.list()
    reviews.value = (res.data || []).map(r => ({
      ...r,
      savedAt: r.savedAt ? dayjs(r.savedAt).format('YYYY-MM-DD HH:mm') : '',
      nextGoals: r.nextGoals ? JSON.parse(r.nextGoals) : [],
      selfScore: r.selfScore || 3
    }))
  } catch { ElMessage.error('회고 기록 로드 실패') }
}

async function saveWeeklyReview() {
  saving.value = true
  try {
    await reviewApi.create({
      type: 'weekly', period: weekLabel.value,
      plans: weeklyForm.plans, progress: weeklyForm.progress,
      problems: weeklyForm.problems, memo: weeklyForm.memo,
      selfScore: weeklyForm.selfScore
    })
    await loadReviews()
    ElMessage.success('주간 회고가 저장되었습니다!')
    clearWeeklyForm()
  } catch { ElMessage.error('저장에 실패했습니다.') } finally { saving.value = false }
}

async function saveMonthlyReview() {
  saving.value = true
  try {
    const goals = monthlyForm.nextGoals.filter(g => g.trim())
    await reviewApi.create({
      type: 'monthly', period: monthLabel.value,
      bestAchievement: monthlyForm.bestAchievement,
      regrets: monthlyForm.regrets,
      nextGoals: JSON.stringify(goals),
      memo: monthlyForm.memo, selfScore: monthlyForm.selfScore
    })
    await loadReviews()
    ElMessage.success('월간 회고가 저장되었습니다!')
    clearMonthlyForm()
  } catch { ElMessage.error('저장에 실패했습니다.') } finally { saving.value = false }
}

async function deleteReview(id) {
  try {
    await reviewApi.delete(id)
    await loadReviews()
  } catch { ElMessage.error('삭제에 실패했습니다.') }
}

function clearWeeklyForm() {
  Object.assign(weeklyForm, { plans: '', progress: '', problems: '', memo: '', selfScore: 3 })
}

function clearMonthlyForm() {
  Object.assign(monthlyForm, { bestAchievement: '', regrets: '', nextGoals: ['', '', ''], memo: '', selfScore: 3 })
}

onMounted(async () => {
  await loadReviews()
  store.fetchDashboard()
  loadEnergyFromServer()
})
</script>

<style scoped>
.page-container { padding: 24px 28px; display: flex; flex-direction: column; gap: 20px; }

/* 탭 */
.review-tabs {
  display: flex; gap: 8px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 12px 16px; box-shadow: var(--shadow-xs);
}
.rtab {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 20px; border-radius: 99px; border: 1.5px solid transparent;
  background: transparent; font-size: 0.88rem; font-weight: 600;
  color: var(--text-secondary); cursor: pointer; transition: all var(--transition-fast);
}
.rtab:hover { background: var(--bg-hover); color: var(--text-primary); }
.rtab.active { background: rgba(48,127,226,0.08); border-color: rgba(48,127,226,0.3); color: var(--color-project); }

/* 히어로 배너 */
.review-hero {
  display: flex; align-items: center; gap: 16px;
  background: linear-gradient(135deg, #1A3A7A 0%, var(--color-project) 100%);
  border-radius: var(--radius-lg); padding: 20px 24px; flex-wrap: wrap;
  box-shadow: var(--shadow-sm);
}
.review-hero-icon {
  width: 52px; height: 52px; border-radius: var(--radius-lg);
  background: rgba(255,255,255,0.18);
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
}
.review-hero-title { font-size: 1.2rem; font-weight: 800; color: white; }
.review-hero-sub { font-size: 0.82rem; color: rgba(255,255,255,0.7); margin-top: 3px; }
.review-hero-kpi { margin-left: auto; }
.hero-stat { text-align: center; }
.hero-stat-val { font-size: 2rem; font-weight: 900; color: white; display: block; font-family: 'JetBrains Mono', monospace; font-variant-numeric: tabular-nums; }
.hero-stat-label { font-size: 0.72rem; color: rgba(255,255,255,0.7); }

/* 회고 폼 */
.review-form {
  display: flex; flex-direction: column; gap: 16px;
}

.review-section {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 20px; box-shadow: var(--shadow-xs);
}

.rs-header {
  display: flex; align-items: flex-start; gap: 12px; margin-bottom: 14px;
}
.rs-icon {
  flex-shrink: 0;
  color: var(--color-project);
  margin-top: 1px;
}
.rs-icon-regret { color: #F43F5E; }
.rs-title { font-size: 0.95rem; font-weight: 700; color: var(--text-primary); }
.rs-hint { font-size: 0.78rem; color: var(--text-muted); margin-top: 3px; }

.score-section-form .el-rate { font-size: 1.5rem; }

.review-actions {
  display: flex; justify-content: flex-end; gap: 10px;
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); padding: 16px 20px; box-shadow: var(--shadow-xs);
}

/* 목표 목록 */
.goals-list { display: flex; flex-direction: column; gap: 10px; }
.goal-input-row { display: flex; align-items: center; gap: 10px; }
.goal-num {
  width: 24px; height: 24px; border-radius: 50%;
  background: rgba(48,127,226,0.10); color: var(--color-project);
  font-size: 0.72rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

/* 회고 기록 없음 */
.empty-reviews {
  display: flex; flex-direction: column; align-items: center; padding: 64px 0; gap: 10px;
}
.er-icon { color: var(--border-color); }
.er-title { font-size: 1.1rem; font-weight: 700; color: var(--text-primary); }
.er-sub { font-size: 0.85rem; color: var(--text-muted); }

/* 회고 기록 목록 */
.review-history { display: flex; flex-direction: column; gap: 14px; }

.review-item {
  background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-xs);
}

.ri-header {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-main);
}

.ri-type-badge {
  font-size: 0.72rem; font-weight: 700; padding: 3px 10px; border-radius: 99px;
}
.ri-type-badge.weekly { background: rgba(48,127,226,0.10); color: var(--color-project); }
.ri-type-badge.monthly { background: rgba(139,92,246,0.10); color: #7C3AED; }

.ri-period { font-size: 0.85rem; font-weight: 600; color: var(--text-primary); }
.ri-score { font-size: 0.85rem; }
.ri-date { font-size: 0.75rem; color: var(--text-muted); margin-left: auto; }

.ri-body { padding: 16px 18px; display: flex; flex-direction: column; gap: 12px; }

.ri-field {}
.ri-field-label {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 0.75rem; font-weight: 700;
  color: var(--text-secondary); margin-bottom: 4px;
}
.ri-field p { font-size: 0.85rem; color: var(--text-primary); line-height: 1.6; white-space: pre-wrap; }
.ri-goals { font-size: 0.85rem; color: var(--text-primary); padding-left: 16px; }
.ri-goals li { margin-bottom: 4px; }

/* 에너지 추이 차트 */
.energy-history-card {
  background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg);
  padding: 18px 20px; margin-bottom: 20px; box-shadow: var(--shadow-xs);
}
.ehc-title {
  font-size: 0.875rem; font-weight: 700; color: var(--text-primary); margin-bottom: 32px;
  display: flex; align-items: center; gap: 8px;
}
.ehc-hint { font-size: 0.75rem; color: var(--text-muted); font-weight: 400; }
.ehc-bars { display: flex; gap: 8px; align-items: flex-end; height: 100px; margin-bottom: 8px; }
.ehc-day { display: flex; flex-direction: column; align-items: center; gap: 4px; flex: 1; }
.ehc-bar-wrap {
  flex: 1; width: 100%; max-width: 40px; background: var(--bg-hover);
  border-radius: var(--radius-sm); overflow: hidden; position: relative; min-height: 60px;
}
.ehc-bar-fill {
  position: absolute; bottom: 0; left: 0; right: 0;
  border-radius: var(--radius-sm); transition: height 0.4s ease;
  min-height: 3px;
}
.ehc-label { font-size: 0.72rem; color: var(--text-muted); font-weight: 500; }
.ehc-label.today { color: var(--color-project); font-weight: 700; }
.ehc-val { font-size: 0.7rem; font-weight: 700; }
.ehc-legend { display: flex; align-items: center; font-size: 0.72rem; color: var(--text-secondary); margin-top: 8px; }
.ehc-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
</style>
