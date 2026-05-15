<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    @submit.prevent="handleSubmit"
  >
    <!-- 템플릿 라이브러리 버튼 (CAT5) -->
    <div class="template-bar" v-if="!kpi">
      <button class="template-btn" @click="showTemplates = !showTemplates">
        📚 템플릿에서 불러오기
        <span class="tmpl-arrow">{{ showTemplates ? '▲' : '▼' }}</span>
      </button>
    </div>

    <!-- 템플릿 패널 -->
    <div v-if="showTemplates && !kpi" class="template-panel">
      <div class="tmpl-cats">
        <button
          v-for="tc in templateCategories"
          :key="tc"
          class="tmpl-cat-btn"
          :class="{ active: selectedTmplCat === tc }"
          @click="selectedTmplCat = tc"
        >{{ tc }}</button>
      </div>
      <div class="tmpl-grid">
        <div
          v-for="tmpl in filteredTemplates"
          :key="tmpl.name"
          class="tmpl-card"
          @click="applyTemplate(tmpl)"
        >
          <div class="tmpl-name">{{ tmpl.name }}</div>
          <div class="tmpl-meta">
            <span class="tmpl-tag">{{ tmpl.frequency === 'DAILY' ? '매일' : tmpl.frequency === 'WEEKLY' ? '매주' : '매월' }}</span>
            <span class="tmpl-tag">{{ tmpl.kpiType === 'NUMERIC' ? '수치' : tmpl.kpiType === 'PERCENTAGE' ? '%' : '달성' }}</span>
            <span v-if="tmpl.targetValue" class="tmpl-tag">목표 {{ tmpl.targetValue }}{{ tmpl.unit }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 카테고리 없을 때 온보딩 안내 -->
    <div v-if="categories.length === 0" class="no-category-guide">
      <div class="guide-icon">📂</div>
      <div class="guide-body">
        <div class="guide-title">카테고리가 없습니다</div>
        <div class="guide-desc">KPI를 추가하려면 먼저 카테고리를 생성해야 합니다.</div>
        <router-link to="/categories" class="guide-link">카테고리 만들러 가기 →</router-link>
      </div>
    </div>

    <div class="form-row">
      <el-form-item label="카테고리" prop="categoryId" class="flex-1">
        <el-select v-model="form.categoryId" placeholder="카테고리 선택" style="width:100%" :disabled="categories.length === 0">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          >
            <span class="flex items-center gap-2">
              <span class="w-2 h-2 rounded-full inline-block" :style="{ background: cat.color }"></span>
              {{ cat.name }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item class="flex-1">
        <template #label>
          KPI 타입
          <el-tooltip placement="top" :content="kpiTypeTooltip" effect="light">
            <span class="type-help-icon">?</span>
          </el-tooltip>
        </template>
        <el-select v-model="form.kpiType" style="width:100%">
          <el-option label="📊 수치 (Numeric)" value="NUMERIC" />
          <el-option label="📈 퍼센트 (0~100%)" value="PERCENTAGE" />
          <el-option label="✅ 달성 여부 (예/아니오)" value="BOOLEAN" />
        </el-select>
        <div class="type-hint">{{ kpiTypeHint }}</div>
      </el-form-item>
    </div>

    <el-form-item label="KPI 이름" prop="name">
      <el-input v-model="form.name" placeholder="예: 하루 걸음 수" maxlength="200" show-word-limit />
    </el-form-item>

    <el-form-item label="설명">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="2"
        placeholder="KPI에 대한 설명을 입력하세요"
        maxlength="1000"
      />
    </el-form-item>

    <div class="form-row" v-if="form.kpiType !== 'BOOLEAN'">
      <el-form-item label="목표값" prop="targetValue" class="flex-1">
        <el-input-number
          v-model="form.targetValue"
          :min="0"
          :precision="form.kpiType === 'PERCENTAGE' ? 1 : 0"
          :max="form.kpiType === 'PERCENTAGE' ? 100 : undefined"
          style="width:100%"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item label="단위" class="flex-1">
        <el-input v-model="form.unit" placeholder="걸음, 분, 만원 등" maxlength="50" />
      </el-form-item>
    </div>

    <div class="form-row">
      <el-form-item label="측정 주기" prop="frequency" class="flex-1">
        <el-select v-model="form.frequency" style="width:100%">
          <el-option label="매일 (Daily)" value="DAILY" />
          <el-option label="매주 (Weekly)" value="WEEKLY" />
          <el-option label="매월 (Monthly)" value="MONTHLY" />
        </el-select>
      </el-form-item>
      <el-form-item label="상태" class="flex-1">
        <el-select v-model="form.status" style="width:100%">
          <el-option label="활성" value="ACTIVE" />
          <el-option label="일시정지" value="PAUSED" />
          <el-option label="완료" value="COMPLETED" />
        </el-select>
      </el-form-item>
    </div>

    <div class="form-row">
      <el-form-item label="시작일" prop="startDate" class="flex-1">
        <el-date-picker
          v-model="form.startDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width:100%"
        />
      </el-form-item>
      <el-form-item label="종료일 (선택)" class="flex-1">
        <el-date-picker
          v-model="form.endDate"
          type="date"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width:100%"
          clearable
        />
      </el-form-item>
    </div>

    <div class="form-footer">
      <el-button @click="$emit('cancel')">취소</el-button>
      <el-button type="primary" native-type="submit">
        {{ kpi ? '수정' : '생성' }}
      </el-button>
    </div>
  </el-form>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import dayjs from 'dayjs'

// ── CAT5: KPI 템플릿 라이브러리 ──
const showTemplates = ref(false)
const selectedTmplCat = ref('전체')

const kpiTemplates = [
  // 영업/매출
  { cat: '영업/매출', name: '월간 매출 달성액', kpiType: 'NUMERIC', targetValue: 1000, unit: '만원', frequency: 'MONTHLY' },
  { cat: '영업/매출', name: '신규 고객 유치', kpiType: 'NUMERIC', targetValue: 10, unit: '건', frequency: 'MONTHLY' },
  { cat: '영업/매출', name: '영업 콜 횟수', kpiType: 'NUMERIC', targetValue: 20, unit: '회', frequency: 'DAILY' },
  { cat: '영업/매출', name: '제안서 발송 건수', kpiType: 'NUMERIC', targetValue: 5, unit: '건', frequency: 'WEEKLY' },
  { cat: '영업/매출', name: '계약 전환율', kpiType: 'PERCENTAGE', targetValue: 30, unit: '', frequency: 'MONTHLY' },
  // 프로젝트
  { cat: '프로젝트', name: '프로젝트 일정 준수율', kpiType: 'PERCENTAGE', targetValue: 90, unit: '', frequency: 'MONTHLY' },
  { cat: '프로젝트', name: '주간 진행 보고', kpiType: 'BOOLEAN', targetValue: null, unit: '', frequency: 'WEEKLY' },
  { cat: '프로젝트', name: '마일스톤 달성', kpiType: 'BOOLEAN', targetValue: null, unit: '', frequency: 'MONTHLY' },
  { cat: '프로젝트', name: '버그 해결 수', kpiType: 'NUMERIC', targetValue: 10, unit: '건', frequency: 'WEEKLY' },
  // 업무효율
  { cat: '업무효율', name: '딥워크 시간', kpiType: 'NUMERIC', targetValue: 4, unit: '시간', frequency: 'DAILY' },
  { cat: '업무효율', name: '업무 처리 건수', kpiType: 'NUMERIC', targetValue: 20, unit: '건', frequency: 'WEEKLY' },
  { cat: '업무효율', name: '이메일 응답률', kpiType: 'PERCENTAGE', targetValue: 95, unit: '', frequency: 'DAILY' },
  { cat: '업무효율', name: 'To-Do 완료율', kpiType: 'PERCENTAGE', targetValue: 80, unit: '', frequency: 'DAILY' },
  // 팀협업
  { cat: '팀협업', name: '팀 미팅 참여', kpiType: 'BOOLEAN', targetValue: null, unit: '', frequency: 'WEEKLY' },
  { cat: '팀협업', name: '동료 코드 리뷰', kpiType: 'NUMERIC', targetValue: 5, unit: '건', frequency: 'WEEKLY' },
  { cat: '팀협업', name: '피드백 제공 횟수', kpiType: 'NUMERIC', targetValue: 3, unit: '회', frequency: 'WEEKLY' },
  // 역량개발
  { cat: '역량개발', name: '교육 이수 시간', kpiType: 'NUMERIC', targetValue: 8, unit: '시간', frequency: 'MONTHLY' },
  { cat: '역량개발', name: '기술 문서 작성', kpiType: 'NUMERIC', targetValue: 2, unit: '건', frequency: 'WEEKLY' },
  { cat: '역량개발', name: '독서량', kpiType: 'NUMERIC', targetValue: 1, unit: '권', frequency: 'MONTHLY' },
  { cat: '역량개발', name: '온라인 강의 수강', kpiType: 'BOOLEAN', targetValue: null, unit: '', frequency: 'WEEKLY' },
  // 건강
  { cat: '건강/생활', name: '하루 걸음 수', kpiType: 'NUMERIC', targetValue: 8000, unit: '보', frequency: 'DAILY' },
  { cat: '건강/생활', name: '운동 횟수', kpiType: 'NUMERIC', targetValue: 3, unit: '회', frequency: 'WEEKLY' },
  { cat: '건강/생활', name: '수면 시간', kpiType: 'NUMERIC', targetValue: 7, unit: '시간', frequency: 'DAILY' },
  { cat: '건강/생활', name: '물 마시기', kpiType: 'NUMERIC', targetValue: 8, unit: '잔', frequency: 'DAILY' },
  { cat: '건강/생활', name: '명상/마음 챙김', kpiType: 'BOOLEAN', targetValue: null, unit: '', frequency: 'DAILY' },
]

const templateCategories = computed(() => ['전체', ...new Set(kpiTemplates.map(t => t.cat))])
const filteredTemplates = computed(() =>
  selectedTmplCat.value === '전체'
    ? kpiTemplates
    : kpiTemplates.filter(t => t.cat === selectedTmplCat.value)
)

function applyTemplate(tmpl) {
  form.name = tmpl.name
  form.kpiType = tmpl.kpiType
  form.targetValue = tmpl.targetValue ?? 0
  form.unit = tmpl.unit
  form.frequency = tmpl.frequency
  showTemplates.value = false
}

const props = defineProps({
  kpi: { type: Object, default: null },
  categories: { type: Array, default: () => [] }
})

const emit = defineEmits(['submit', 'cancel'])

const formRef = ref()

const form = reactive({
  categoryId: null,
  name: '',
  description: '',
  unit: '',
  kpiType: 'NUMERIC',
  targetValue: 0,
  frequency: 'DAILY',
  startDate: dayjs().format('YYYY-MM-DD'),
  endDate: null,
  status: 'ACTIVE',
  sortOrder: 0
})

watch(() => props.kpi, (val) => {
  if (val) {
    Object.assign(form, {
      categoryId: val.categoryId,
      name: val.name,
      description: val.description || '',
      unit: val.unit || '',
      kpiType: val.kpiType || 'NUMERIC',
      targetValue: val.targetValue || 0,
      frequency: val.frequency || 'DAILY',
      startDate: val.startDate,
      endDate: val.endDate || null,
      status: val.status || 'ACTIVE',
      sortOrder: val.sortOrder || 0
    })
  }
}, { immediate: true })

const kpiTypeHint = computed(() => ({
  NUMERIC:    '운동 횟수, 매출액, 독서 페이지 수 등 구체적인 수치를 입력합니다.',
  PERCENTAGE: '달성률, 완료율 등 0~100% 사이의 비율로 측정합니다.',
  BOOLEAN:    '회의 참석, 보고서 제출 등 완료 여부(예/아니오)만 기록합니다.'
}[form.kpiType] ?? ''))

const kpiTypeTooltip = computed(() => ({
  NUMERIC:    '📊 수치: 목표값 대비 실적을 숫자로 기록',
  PERCENTAGE: '📈 퍼센트: 0~100% 범위의 비율 KPI',
  BOOLEAN:    '✅ 달성 여부: 했다/안 했다로만 기록'
}[form.kpiType] ?? 'KPI 측정 방식을 선택하세요'))

const rules = {
  categoryId: [{ required: true, message: '카테고리를 선택하세요' }],
  name: [{ required: true, message: 'KPI 이름을 입력하세요', min: 1, max: 200 }],
  kpiType: [{ required: true, message: 'KPI 타입을 선택하세요' }],
  frequency: [{ required: true, message: '측정 주기를 선택하세요' }],
  startDate: [{ required: true, message: '시작일을 선택하세요' }],
  targetValue: [
    {
      validator: (rule, value, callback) => {
        if (form.kpiType !== 'BOOLEAN' && (value === null || value === undefined)) {
          callback(new Error('목표값을 입력하세요'))
        } else {
          callback()
        }
      }
    }
  ]
}

async function handleSubmit() {
  await formRef.value.validate()
  const payload = { ...form }
  if (payload.kpiType === 'BOOLEAN') {
    payload.targetValue = null
    payload.unit = null
  }
  emit('submit', payload)
}
</script>

<style scoped>
.form-row {
  display: flex;
  gap: 16px;
}

.form-row > * {
  flex: 1;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

/* ── 카테고리 없을 때 안내 ── */
.no-category-guide {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: rgba(245,158,11,0.06);
  border: 1px solid rgba(245,158,11,0.3);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-bottom: 16px;
}
.guide-icon { font-size: 1.4rem; line-height: 1; flex-shrink: 0; margin-top: 2px; }
.guide-title { font-weight: 700; color: var(--color-warning); font-size: 0.9rem; margin-bottom: 3px; }
.guide-desc  { font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 8px; }
.guide-link  {
  font-size: 0.8rem; font-weight: 600; color: var(--color-warning);
  text-decoration: none;
}
.guide-link:hover { text-decoration: underline; }

/* ── 템플릿 라이브러리 ── */
.template-bar { margin-bottom: 12px; }
.template-btn {
  display: flex; align-items: center; gap: 6px;
  font-size: 0.83rem; font-weight: 600; color: var(--color-project);
  background: rgba(48,127,226,0.08); border: 1.5px solid rgba(48,127,226,0.25);
  border-radius: var(--radius-md); padding: 7px 14px; cursor: pointer;
  transition: all var(--transition-fast);
}
.template-btn:hover { background: rgba(48,127,226,0.14); }
.tmpl-arrow { font-size: 0.7rem; }

.template-panel {
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 14px; margin-bottom: 16px;
}

.tmpl-cats { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.tmpl-cat-btn {
  padding: 4px 12px; border-radius: 99px; border: 1.5px solid var(--border-color);
  background: var(--bg-card); font-size: 0.75rem; font-weight: 600; color: var(--text-secondary);
  cursor: pointer; transition: all var(--transition-fast);
}
.tmpl-cat-btn.active, .tmpl-cat-btn:hover {
  background: rgba(48,127,226,0.08); border-color: rgba(48,127,226,0.35); color: var(--color-project);
}

.tmpl-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px;
  max-height: 200px; overflow-y: auto;
}

.tmpl-card {
  background: var(--bg-card); border: 1.5px solid var(--border-color);
  border-radius: var(--radius-md); padding: 10px 12px;
  cursor: pointer; transition: all var(--transition-fast);
}
.tmpl-card:hover { border-color: var(--color-project); background: rgba(48,127,226,0.06); }
.tmpl-name { font-size: 0.82rem; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.tmpl-meta { display: flex; flex-wrap: wrap; gap: 4px; }
.tmpl-tag {
  font-size: 0.65rem; background: var(--bg-hover); color: var(--text-secondary);
  padding: 1px 6px; border-radius: var(--radius-xs); font-weight: 500;
}

/* ── KPI 타입 도움말 ── */
.type-help-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border-radius: 50%;
  background: var(--bg-hover); color: var(--text-secondary);
  font-size: 0.65rem; font-weight: 700;
  margin-left: 5px; cursor: help;
  vertical-align: middle;
}
.type-hint {
  font-size: 0.73rem;
  color: var(--text-secondary);
  margin-top: 5px;
  line-height: 1.4;
  min-height: 18px;
}
</style>
