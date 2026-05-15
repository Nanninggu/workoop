<template>
  <div class="page-container">

    <!-- ① 앱 정보 -->
    <div class="settings-section">
      <div class="section-header">
        <Info :size="18" class="section-icon" />
        <h2 class="section-title">앱 정보</h2>
      </div>
      <!-- 로고 -->
      <div class="app-logo-row">
        <div class="app-logo-inline">
          <div class="app-logo-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 44 44" width="44" height="44">
              <defs>
                <linearGradient id="sl-bg" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#4F46E5"/>
                  <stop offset="100%" stop-color="#7C3AED"/>
                </linearGradient>
              </defs>
              <rect width="44" height="44" rx="10" fill="url(#sl-bg)"/>
              <path d="M5.5 9.5 L11.5 28.5 L22 14.5 L32.5 28.5 L38.5 9.5"
                    stroke="white" stroke-width="3.8" fill="none"
                    stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M16.5 36 A5.5 5.5 0 1 1 22 30.5"
                    stroke="rgba(255,255,255,0.88)" stroke-width="2.2" fill="none"
                    stroke-linecap="round"/>
              <path d="M19 28 L22 30.5 L19 33"
                    stroke="rgba(255,255,255,0.88)" stroke-width="2.2" fill="none"
                    stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="app-logo-text-wrap">
            <span class="app-logo-name" style="color:#4F46E5; font-weight:800; font-size:22px; letter-spacing:-0.5px;">Workoop</span>
            <span class="app-logo-sub">팀 협업 & KPI 관리 플랫폼</span>
          </div>
        </div>
        <div class="app-logo-tagline">KPI 연동 칸반, 데일리 스크럼, 번아웃 방지까지 한 곳에서.</div>
      </div>
      <div class="info-grid">
        <div class="info-row">
          <span class="info-key">앱 이름</span>
          <span class="info-val">Workoop — 팀 협업 & KPI 관리 플랫폼</span>
        </div>
        <div class="info-row">
          <span class="info-key">버전</span>
          <span class="info-val"><el-tag size="small" type="success">v1.0.0</el-tag></span>
        </div>
        <div class="info-row">
          <span class="info-key">기술 스택</span>
          <span class="info-val">
            <el-tag size="small" class="mr-1">Vue.js 3</el-tag>
            <el-tag size="small" class="mr-1">Spring Boot 3</el-tag>
            <el-tag size="small">H2 DB</el-tag>
          </span>
        </div>
        <div class="info-row">
          <span class="info-key">Backend API</span>
          <a href="http://localhost:8080" target="_blank" class="info-link">http://localhost:8080</a>
        </div>
        <div class="info-row">
          <span class="info-key">H2 Console</span>
          <a href="http://localhost:8080/h2-console" target="_blank" class="info-link">
            http://localhost:8080/h2-console
          </a>
        </div>
      </div>
    </div>

    <!-- ② 데이터 내보내기 -->
    <div class="settings-section">
      <div class="section-header">
        <Download :size="18" class="section-icon" />
        <h2 class="section-title">데이터 내보내기</h2>
      </div>
      <p class="section-desc">원하는 기간의 KPI 기록을 CSV 파일로 내보냅니다.</p>
      <div class="export-form">
        <div class="export-date-row">
          <div class="export-field">
            <label class="field-label">시작일</label>
            <el-date-picker
              v-model="exportStartDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="시작일 선택"
              :clearable="false"
              style="width: 100%"
            />
          </div>
          <div class="export-field">
            <label class="field-label">종료일</label>
            <el-date-picker
              v-model="exportEndDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="종료일 선택"
              :clearable="false"
              :disabled-date="(d) => d < new Date(exportStartDate)"
              style="width: 100%"
            />
          </div>
        </div>
        <div class="export-shortcuts">
          <el-button size="small" @click="setExportRange(7)">최근 7일</el-button>
          <el-button size="small" @click="setExportRange(30)">최근 30일</el-button>
          <el-button size="small" @click="setExportRange(90)">최근 3개월</el-button>
          <el-button size="small" @click="setExportThisMonth">이번 달</el-button>
        </div>
        <el-button type="primary" @click="doExport" :loading="exporting">
          <Download :size="15" class="mr-1" /> CSV 내보내기
        </el-button>
      </div>
    </div>

    <!-- ③ KPI 요약 현황 -->
    <div class="settings-section">
      <div class="section-header">
        <BarChart3 :size="18" class="section-icon" />
        <h2 class="section-title">KPI 현황 요약</h2>
      </div>
      <div v-if="loadingStats" class="flex justify-center py-8">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
      </div>
      <div v-else class="stats-summary-grid">
        <div class="stat-summary-card">
          <div class="stat-summary-num">{{ stats.totalKpis }}</div>
          <div class="stat-summary-label">전체 KPI</div>
        </div>
        <div class="stat-summary-card">
          <div class="stat-summary-num text-emerald-600">{{ stats.activeKpis }}</div>
          <div class="stat-summary-label">활성 KPI</div>
        </div>
        <div class="stat-summary-card">
          <div class="stat-summary-num text-amber-500">{{ stats.totalCategories }}</div>
          <div class="stat-summary-label">카테고리</div>
        </div>
        <div class="stat-summary-card">
          <div class="stat-summary-num text-blue-500">{{ stats.overallAchievementRate }}%</div>
          <div class="stat-summary-label">평균 달성률</div>
        </div>
      </div>
    </div>

    <!-- ④ 데이터 백업/복원 (CAT9) -->
    <div class="settings-section">
      <div class="section-header">
        <Database :size="18" class="section-icon" />
        <h2 class="section-title">데이터 백업 / 복원</h2>
      </div>
      <p class="section-desc">모든 카테고리, KPI, 기록 데이터를 JSON 파일로 백업하고 복원할 수 있습니다.</p>

      <div class="backup-actions">
        <!-- JSON 백업 -->
        <div class="backup-card">
          <div class="bc-icon" style="background:#EFF6FF; color:#2563EB">📥</div>
          <div class="bc-info">
            <div class="bc-title">JSON 전체 백업</div>
            <div class="bc-desc">모든 데이터를 JSON 파일로 내보냅니다. 안전하게 보관하세요.</div>
          </div>
          <el-button type="primary" @click="backupJson" :loading="backingUp">
            백업 다운로드
          </el-button>
        </div>

        <!-- JSON 복원 -->
        <div class="backup-card">
          <div class="bc-icon" style="background:#FEF3C7; color:#D97706">📤</div>
          <div class="bc-info">
            <div class="bc-title">JSON 복원</div>
            <div class="bc-desc">
              <span class="warn-text">⚠️ 주의: 기존 모든 데이터가 삭제되고 백업 데이터로 대체됩니다.</span>
            </div>
          </div>
          <div class="restore-actions">
            <input ref="fileInputRef" type="file" accept=".json" style="display:none" @change="onJsonFileChange" />
            <el-button @click="fileInputRef?.click()">파일 선택</el-button>
            <el-button type="warning" :disabled="!importFile" @click="confirmRestore" :loading="restoring">
              복원 실행
            </el-button>
          </div>
        </div>

        <!-- PDF 출력 (CAT9: DAT-3) -->
        <div class="backup-card">
          <div class="bc-icon" style="background:#FDF4FF; color:#A855F7">🖨️</div>
          <div class="bc-info">
            <div class="bc-title">PDF 리포트 출력</div>
            <div class="bc-desc">현재 대시보드를 PDF로 출력합니다. 브라우저 인쇄 기능을 활용합니다.</div>
          </div>
          <el-button @click="printPdf">PDF 출력</el-button>
        </div>
      </div>

      <!-- 복원 파일 선택 표시 -->
      <div v-if="importFile" class="import-file-info">
        선택된 파일: <strong>{{ importFile.name }}</strong> ({{ (importFile.size / 1024).toFixed(1) }} KB)
      </div>
    </div>

    <!-- ⑤ 배지 컬렉션 (CAT8) -->
    <div class="settings-section">
      <div class="section-header">
        <Trophy :size="18" class="section-icon" />
        <h2 class="section-title">획득한 배지</h2>
      </div>
      <div class="badge-collection" v-if="earnedBadges.length > 0">
        <div v-for="b in allBadges" :key="b.id" class="badge-tile" :class="{ earned: earnedBadges.includes(b.id) }">
          <div class="bt-emoji">{{ b.emoji }}</div>
          <div class="bt-name">{{ b.name }}</div>
          <div class="bt-desc">{{ b.desc }}</div>
        </div>
      </div>
      <div v-else class="no-data-msg">아직 획득한 배지가 없습니다. KPI를 꾸준히 관리해보세요!</div>
    </div>

    <!-- ⑥ 빠른 링크 -->
    <div class="settings-section">
      <div class="section-header">
        <Link2 :size="18" class="section-icon" />
        <h2 class="section-title">빠른 링크</h2>
      </div>
      <div class="quick-links">
        <router-link to="/dashboard" class="quick-link-card">
          <LayoutDashboard :size="20" />
          <span>대시보드</span>
        </router-link>
        <router-link to="/analytics" class="quick-link-card">
          <LineChart :size="20" />
          <span>분석</span>
        </router-link>
        <router-link to="/calendar" class="quick-link-card">
          <CalendarDays :size="20" />
          <span>캘린더</span>
        </router-link>
        <router-link to="/kpis" class="quick-link-card">
          <BarChart3 :size="20" />
          <span>KPI 관리</span>
        </router-link>
        <router-link to="/records" class="quick-link-card">
          <ListChecks :size="20" />
          <span>실적 입력</span>
        </router-link>
        <router-link to="/categories" class="quick-link-card">
          <Tag :size="20" />
          <span>카테고리 관리</span>
        </router-link>
      </div>
    </div>

    <!-- ── 팀 멤버 관리 ── -->
    <div class="settings-section" v-if="orgStore.currentOrg">
      <div class="section-header">
        <Users :size="18" class="section-icon" />
        <h2 class="section-title">팀 멤버 — {{ orgStore.currentOrg.name }}</h2>
      </div>

      <!-- 초대 코드 -->
      <div class="invite-box">
        <div class="invite-label">초대 코드</div>
        <div v-if="currentOrg?.inviteCode && isInviteValid" class="invite-code-row">
          <span class="invite-code">{{ currentOrg.inviteCode }}</span>
          <button class="btn-copy" @click="copyCode">
            <Copy :size="14" /> {{ copied ? '복사됨!' : '복사' }}
          </button>
          <span class="invite-expire">{{ inviteExpireLabel }} 만료</span>
        </div>
        <div v-else class="invite-empty">아직 초대 코드가 없습니다.</div>
        <button class="btn-gen" :disabled="generating" @click="generateInvite">
          <RefreshCw :size="14" /> {{ generating ? '생성 중...' : '새 코드 발급 (24시간)' }}
        </button>
        <p class="invite-hint">팀원에게 코드를 공유하면, 로그인 후 설정에서 코드를 입력해 합류할 수 있습니다.</p>
      </div>

      <!-- 코드로 조직 참여 -->
      <div class="join-box" v-if="!orgStore.currentOrg">
        <div class="invite-label">초대 코드로 참여</div>
        <div class="join-row">
          <input v-model="joinCode" placeholder="초대 코드 입력 (예: AB3XYZ)" class="join-input" maxlength="8" />
          <button class="btn-join" :disabled="joining || !joinCode.trim()" @click="joinOrg">
            {{ joining ? '참여 중...' : '참여하기' }}
          </button>
        </div>
      </div>

      <!-- 멤버 목록 -->
      <div class="member-list">
        <div v-for="m in members" :key="m.userId" class="member-row">
          <div class="member-avatar">{{ (m.userName || '?').charAt(0).toUpperCase() }}</div>
          <div class="member-info">
            <div class="member-name">{{ m.userName }}</div>
            <div class="member-email">{{ m.userEmail }}</div>
          </div>
          <div class="member-meta">
            <span class="member-joined">{{ formatDate(m.joinedAt) }} 가입</span>
            <el-select
              v-if="myRole !== 'MEMBER' && m.userId !== authStore.user?.id"
              v-model="m.role"
              size="small"
              style="width:100px"
              @change="(val) => onRoleChange(m.userId, val)"
            >
              <el-option label="Owner" value="OWNER" />
              <el-option label="Admin" value="ADMIN" />
              <el-option label="Member" value="MEMBER" />
            </el-select>
            <span v-else class="role-badge" :class="m.role.toLowerCase()">{{ m.role }}</span>
            <button
              v-if="myRole !== 'MEMBER' && m.userId !== authStore.user?.id"
              class="btn-remove"
              @click="onRemoveMember(m.userId, m.userName)"
            >제거</button>
          </div>
        </div>
        <div v-if="members.length === 0" class="member-empty">멤버가 없습니다.</div>
      </div>
    </div>

    <!-- 조직 없을 때 참여 박스 -->
    <div class="settings-section" v-else>
      <div class="section-header">
        <Users :size="18" class="section-icon" />
        <h2 class="section-title">팀 참여</h2>
      </div>
      <div class="join-box">
        <div class="invite-label">초대 코드로 팀에 합류</div>
        <div class="join-row">
          <input v-model="joinCode" placeholder="초대 코드 입력 (예: AB3XYZ)" class="join-input" maxlength="8" />
          <button class="btn-join" :disabled="joining || !joinCode.trim()" @click="joinOrg">
            {{ joining ? '참여 중...' : '참여하기' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { exportApi, dashboardApi } from '@/api/kpiApi'
import { categoryApi } from '@/api/categoryApi'
import { orgApi } from '@/api/orgApi'
import { useOrgStore } from '@/store/orgStore'
import { useAuthStore } from '@/store/authStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  Info, Download, BarChart3, Link2, Database, Trophy,
  LayoutDashboard, ListChecks, Tag, LineChart, CalendarDays,
  Users, Copy, RefreshCw
} from 'lucide-vue-next'

const orgStore  = useOrgStore()
const authStore = useAuthStore()

// ── 팀 멤버 ──
const members    = ref([])
const currentOrg = ref(null)
const generating = ref(false)
const copied     = ref(false)
const joinCode   = ref('')
const joining    = ref(false)

const myRole = computed(() => {
  const me = members.value.find(m => m.userId === authStore.user?.id)
  return me?.role || 'MEMBER'
})

const isInviteValid = computed(() => {
  if (!currentOrg.value?.inviteExpiresAt) return false
  return dayjs(currentOrg.value.inviteExpiresAt).isAfter(dayjs())
})

const inviteExpireLabel = computed(() => {
  if (!currentOrg.value?.inviteExpiresAt) return ''
  return dayjs(currentOrg.value.inviteExpiresAt).format('MM/DD HH:mm')
})

async function loadOrgData() {
  if (!orgStore.currentOrg) return
  const [orgRes, membersRes] = await Promise.all([
    orgApi.list(),
    orgApi.members(orgStore.currentOrg.id)
  ])
  const found = orgRes.data?.find(o => o.id === orgStore.currentOrg.id)
  if (found) currentOrg.value = found
  members.value = membersRes.data || []
}

async function generateInvite() {
  generating.value = true
  try {
    const res = await orgApi.generateInvite(orgStore.currentOrg.id)
    currentOrg.value = res.data
    ElMessage.success('초대 코드가 발급되었습니다.')
  } finally {
    generating.value = false
  }
}

async function copyCode() {
  await navigator.clipboard.writeText(currentOrg.value.inviteCode)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

async function joinOrg() {
  joining.value = true
  try {
    const res = await orgApi.joinByCode(joinCode.value.trim().toUpperCase())
    orgStore.setCurrentOrg(res.data)
    await orgStore.fetchOrgs()
    ElMessage.success(`${res.data.name} 조직에 참여했습니다!`)
    joinCode.value = ''
    await loadOrgData()
  } finally {
    joining.value = false
  }
}

async function onRoleChange(userId, role) {
  await orgApi.changeRole(orgStore.currentOrg.id, userId, role)
  ElMessage.success('역할이 변경되었습니다.')
}

async function onRemoveMember(userId, name) {
  await ElMessageBox.confirm(`${name}을 조직에서 제거할까요?`, '멤버 제거', {
    confirmButtonText: '제거', cancelButtonText: '취소', type: 'warning'
  })
  await orgApi.removeMember(orgStore.currentOrg.id, userId)
  members.value = members.value.filter(m => m.userId !== userId)
  ElMessage.success('멤버가 제거되었습니다.')
}

function formatDate(dt) {
  return dt ? dayjs(dt).format('YYYY.MM.DD') : ''
}

const exporting = ref(false)
const loadingStats = ref(false)

// ── CAT9: 백업/복원 ──
const backingUp = ref(false)
const restoring = ref(false)
const fileInputRef = ref()
const importFile = ref(null)

async function backupJson() {
  backingUp.value = true
  try { exportApi.json() } finally {
    setTimeout(() => { backingUp.value = false }, 1000)
  }
}

function onJsonFileChange(e) {
  importFile.value = e.target.files[0] || null
}

async function confirmRestore() {
  try {
    await ElMessageBox.confirm(
      '기존 모든 데이터(카테고리, KPI, 기록)가 삭제되고 백업 데이터로 복원됩니다. 계속하시겠습니까?',
      '데이터 복원 확인',
      { confirmButtonText: '복원 실행', cancelButtonText: '취소', type: 'warning' }
    )
    restoring.value = true
    const text = await importFile.value.text()
    const data = JSON.parse(text)
    const res = await exportApi.importJson(data)
    const counts = res.data
    ElMessage.success(`복원 완료: 카테고리 ${counts.categories}개, KPI ${counts.kpis}개, 기록 ${counts.records}건`)
    importFile.value = null
    if (fileInputRef.value) fileInputRef.value.value = ''
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('복원 실패: ' + (e?.message || '파일 형식을 확인하세요'))
  } finally {
    restoring.value = false
  }
}

function printPdf() {
  window.print()
}

// ── CAT8: 배지 컬렉션 표시 ──
const BADGE_KEY = 'workoop-badges'
const earnedBadges = ref(JSON.parse(localStorage.getItem(BADGE_KEY) || '[]'))
const allBadges = [
  { id: 'first_kpi',    emoji: '🥇', name: '첫 KPI 달성',   desc: '첫 번째 KPI 기록을 완료했습니다!' },
  { id: 'streak_7',     emoji: '🔥', name: '7일 연속 달성',  desc: '7일 연속으로 KPI를 달성했습니다!' },
  { id: 'streak_30',    emoji: '💎', name: '30일 연속 달성', desc: '30일 연속 달성! 놀라운 꾸준함입니다!' },
  { id: 'perfect_week', emoji: '⭐', name: '완벽한 한 주',   desc: '이번 주 모든 KPI를 100% 달성했습니다!' },
  { id: 'active_10',    emoji: '🚀', name: '10개 KPI 운영',  desc: '10개 이상의 KPI를 관리하고 있습니다!' },
]

const exportStartDate = ref(dayjs().subtract(30, 'day').format('YYYY-MM-DD'))
const exportEndDate   = ref(dayjs().format('YYYY-MM-DD'))

const stats = ref({
  totalKpis: 0,
  activeKpis: 0,
  totalCategories: 0,
  overallAchievementRate: 0
})

function setExportRange(days) {
  exportStartDate.value = dayjs().subtract(days, 'day').format('YYYY-MM-DD')
  exportEndDate.value   = dayjs().format('YYYY-MM-DD')
}

function setExportThisMonth() {
  exportStartDate.value = dayjs().startOf('month').format('YYYY-MM-DD')
  exportEndDate.value   = dayjs().format('YYYY-MM-DD')
}

function doExport() {
  if (!exportStartDate.value || !exportEndDate.value) {
    ElMessage.warning('시작일과 종료일을 모두 선택해주세요.')
    return
  }
  exporting.value = true
  try {
    exportApi.csv(exportStartDate.value, exportEndDate.value)
    ElMessage.success(`${exportStartDate.value} ~ ${exportEndDate.value} CSV 파일을 다운로드합니다.`)
  } finally {
    setTimeout(() => { exporting.value = false }, 1000)
  }
}

async function loadStats() {
  loadingStats.value = true
  try {
    const [dashRes, catRes] = await Promise.all([
      dashboardApi.get(),
      categoryApi.getAll()
    ])
    const d = dashRes.data
    stats.value = {
      totalKpis: d.totalKpis ?? 0,
      activeKpis: d.activeKpis ?? 0,
      totalCategories: (catRes.data || []).length,
      overallAchievementRate: d.overallAchievementRate ?? 0
    }
  } catch {
    // ignore
  } finally {
    loadingStats.value = false
  }
}

onMounted(() => {
  loadStats()
  loadOrgData()
})
</script>

<style scoped>
.page-container { padding: 24px 28px; max-width: 800px; }

/* ── 섹션 공통 ── */
.settings-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 22px 24px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-xs);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-color);
}

.section-icon { color: var(--color-project); flex-shrink: 0; }

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.section-desc {
  font-size: 0.83rem;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

/* ── 로고 ── */
.app-logo-row {
  display: flex; flex-direction: column; align-items: flex-start;
  gap: 8px; margin-bottom: 20px;
}
.app-logo-inline {
  display: flex; align-items: center; gap: 12px;
}
.app-logo-icon {
  width: 48px; height: 48px;
  background: linear-gradient(135deg, #1A3A7A, var(--color-project));
  border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(48,127,226,0.28);
  flex-shrink: 0;
}
.app-logo-text-wrap {
  display: flex; flex-direction: column; gap: 2px;
}
.app-logo-name {
  font-size: 1.4rem; font-weight: 900; letter-spacing: -0.5px; line-height: 1;
}
.app-logo-sub {
  font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 500;
}
.app-logo-tagline { font-size: 0.82rem; color: var(--text-secondary); }

/* ── 앱 정보 ── */
.info-grid { display: flex; flex-direction: column; gap: 10px; }
.info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.88rem;
}
.info-key {
  width: 110px;
  flex-shrink: 0;
  font-weight: 600;
  color: var(--text-secondary);
}
.info-val { color: var(--text-primary); }
.info-link {
  color: var(--color-project);
  text-decoration: none;
  font-size: 0.85rem;
}
.info-link:hover { text-decoration: underline; }

/* ── 내보내기 ── */
.export-form { display: flex; flex-direction: column; gap: 14px; }
.export-date-row { display: flex; gap: 16px; }
.export-field { flex: 1; }
.field-label {
  display: block;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}
.export-shortcuts { display: flex; gap: 8px; flex-wrap: wrap; }

/* ── KPI 요약 ── */
.stats-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
@media (max-width: 640px) { .stats-summary-grid { grid-template-columns: repeat(2, 1fr); } }
.stat-summary-card {
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px;
  text-align: center;
}
.stat-summary-num {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  margin-bottom: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-variant-numeric: tabular-nums;
}
.stat-summary-label { font-size: 0.75rem; color: var(--text-secondary); font-weight: 500; }

/* ── 백업/복원 ── */
.backup-actions { display: flex; flex-direction: column; gap: 12px; }
.backup-card {
  display: flex; align-items: center; gap: 14px;
  border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 14px 18px;
  background: var(--bg-hover); flex-wrap: wrap;
}
.bc-icon { font-size: 1.6rem; flex-shrink: 0; }
.bc-info { flex: 1; min-width: 180px; }
.bc-title { font-size: 0.92rem; font-weight: 700; color: var(--text-primary); margin-bottom: 3px; }
.bc-desc { font-size: 0.8rem; color: var(--text-secondary); }
.warn-text { color: var(--color-warning); }
.restore-actions { display: flex; gap: 6px; }
.import-file-info {
  margin-top: 8px; font-size: 0.82rem; color: var(--text-secondary);
  background: rgba(48,127,226,0.06); border-radius: var(--radius-md); padding: 8px 12px;
}

/* ── 배지 컬렉션 ── */
.badge-collection { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; }
.badge-tile {
  border: 1.5px solid var(--border-color); border-radius: var(--radius-md); padding: 14px;
  text-align: center; opacity: 0.35; transition: all var(--transition-base);
  background: var(--bg-hover);
}
.badge-tile.earned { opacity: 1; background: var(--bg-card); border-color: rgba(251,191,36,0.5); box-shadow: 0 2px 8px rgba(251,191,36,0.18); }
.bt-emoji { font-size: 2rem; margin-bottom: 6px; }
.bt-name { font-size: 0.82rem; font-weight: 700; color: var(--text-primary); margin-bottom: 3px; }
.bt-desc { font-size: 0.7rem; color: var(--text-secondary); }

.no-data-msg { font-size: 0.85rem; color: var(--text-muted); padding: 16px 0; }

/* ── 빠른 링크 ── */
.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}
.quick-link-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 600;
  transition: all var(--transition-fast);
}
.quick-link-card:hover {
  border-color: rgba(48,127,226,0.4);
  background: rgba(48,127,226,0.06);
  color: var(--color-project);
}

/* ── 팀 멤버 ── */
.invite-box {
  background: var(--bg-hover); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); padding: 16px 20px; margin-bottom: 20px;
}
.invite-label { font-size: 0.78rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
.invite-code-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.invite-code { font-size: 1.5rem; font-weight: 800; letter-spacing: 4px; color: var(--text-primary); font-family: 'JetBrains Mono', monospace; }
.btn-copy {
  display: flex; align-items: center; gap: 5px;
  padding: 5px 12px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-size: 0.8rem; font-weight: 600; cursor: pointer;
}
.btn-copy:hover { opacity: 0.88; }
.invite-expire { font-size: 0.78rem; color: var(--text-muted); }
.invite-empty { font-size: 0.875rem; color: var(--text-muted); margin-bottom: 10px; }
.btn-gen {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: var(--bg-card); border: 1px solid var(--border-color);
  border-radius: var(--radius-md); font-size: 0.825rem; font-weight: 600;
  color: var(--text-secondary); cursor: pointer; margin-top: 8px;
}
.btn-gen:hover:not(:disabled) { border-color: var(--gray-300); }
.btn-gen:disabled { opacity: 0.5; cursor: not-allowed; }
.invite-hint { font-size: 0.75rem; color: var(--text-muted); margin-top: 10px; line-height: 1.5; }

.join-box { background: var(--bg-hover); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 16px 20px; margin-bottom: 20px; }
.join-row { display: flex; gap: 8px; }
.join-input {
  flex: 1; padding: 9px 14px; border: 1px solid var(--border-color);
  border-radius: var(--radius-md); font-size: 0.9rem; outline: none;
  text-transform: uppercase; letter-spacing: 2px; font-weight: 700;
  background: var(--bg-card); color: var(--text-primary);
}
.join-input:focus { border-color: var(--color-project); }
.btn-join {
  padding: 9px 18px; background: var(--color-project); color: white;
  border: none; border-radius: var(--radius-md); font-weight: 600; cursor: pointer;
  white-space: nowrap;
}
.btn-join:hover:not(:disabled) { opacity: 0.88; }
.btn-join:disabled { opacity: 0.5; cursor: not-allowed; }

.member-list { display: flex; flex-direction: column; gap: 8px; }
.member-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; background: var(--bg-hover); border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}
.member-avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-project), #8B5CF6);
  color: white; font-size: 0.9rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.member-info { flex: 1; min-width: 0; }
.member-name { font-size: 0.9rem; font-weight: 600; color: var(--text-primary); }
.member-email { font-size: 0.75rem; color: var(--text-muted); }
.member-meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.member-joined { font-size: 0.75rem; color: var(--text-muted); }
.role-badge {
  padding: 2px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 700;
}
.role-badge.owner { background: rgba(217,119,6,0.12); color: #D97706; }
.role-badge.admin { background: rgba(124,58,237,0.10); color: #7C3AED; }
.role-badge.member { background: rgba(2,132,199,0.10); color: #0284C7; }
.btn-remove {
  padding: 4px 10px; background: transparent; border: 1px solid rgba(239,68,68,0.4);
  border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--color-danger); cursor: pointer;
}
.btn-remove:hover { background: rgba(239,68,68,0.06); }
.member-empty { text-align: center; padding: 24px; color: var(--text-muted); font-size: 0.875rem; }
</style>
