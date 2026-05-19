<template>
  <div class="app-layout">
    <!-- 사이드바 -->
    <aside class="app-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <router-link to="/dashboard" class="sidebar-logo">
        <div class="logo-icon">
          <img src="/logo.svg" alt="Workoop" />
        </div>
        <div v-if="!sidebarCollapsed" class="logo-info">
          <span class="logo-text">Workoop</span>
          <span v-if="orgStore.currentOrg" class="logo-org">{{ orgStore.currentOrg.name }}</span>
        </div>
      </router-link>

      <nav class="sidebar-nav">
        <template v-for="group in navGroups" :key="group.label">
          <span v-if="!sidebarCollapsed" class="nav-group-label">{{ group.label }}</span>
          <div v-else class="nav-group-divider"></div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            active-class="nav-item--active"
          >
            <component :is="item.icon" :size="20" />
            <span v-if="!sidebarCollapsed" class="nav-label">{{ item.label }}</span>
            <el-tooltip v-if="sidebarCollapsed" :content="item.label" placement="right">
              <span></span>
            </el-tooltip>
          </router-link>
        </template>
      </nav>

      <!-- 팀 멤버 아바타 미니 표시 -->
      <div v-if="teamMembers.length > 0 && !sidebarCollapsed" class="sidebar-members">
        <div
          v-for="(m, i) in teamMembers.slice(0, 5)"
          :key="m.userId"
          class="sidebar-member-avatar"
          :title="m.userName"
          :style="{ zIndex: 10 - i }"
        >{{ (m.userName || '?').charAt(0).toUpperCase() }}</div>
        <div v-if="teamMembers.length > 5" class="sidebar-member-more">
          +{{ teamMembers.length - 5 }}
        </div>
      </div>

      <!-- 집중 모드 버튼 -->
      <button
        class="focus-toggle"
        :class="{ active: focusStore.active }"
        @click="focusStore.active ? focusStore.exit() : focusStore.enter()"
        :title="focusStore.active ? '집중 모드 종료 (F)' : '집중 모드 시작 (F)'"
      >
        <Headphones :size="18" />
        <span v-if="!sidebarCollapsed" class="focus-toggle-label">
          {{ focusStore.active ? '집중 중...' : '집중 모드' }}
        </span>
        <el-tooltip v-if="sidebarCollapsed" content="집중 모드 (F)" placement="right">
          <span></span>
        </el-tooltip>
      </button>

      <!-- 채팅 버튼 -->
      <button
        class="chat-toggle"
        :class="{ active: chatOpen }"
        @click="toggleChat"
        :title="chatOpen ? '채팅 닫기' : '팀 채팅 (C)'"
      >
        <MessageSquare :size="18" />
        <span v-if="!sidebarCollapsed" class="chat-toggle-label">팀 채팅</span>
        <span v-if="!sidebarCollapsed && chatStore.unread > 0" class="chat-unread-badge">
          {{ chatStore.unread > 9 ? '9+' : chatStore.unread }}
        </span>
        <el-tooltip v-if="sidebarCollapsed" content="팀 채팅 (C)" placement="right">
          <span></span>
        </el-tooltip>
      </button>

      <!-- 메모 버튼 -->
      <button
        class="memo-toggle"
        :class="{ active: memoOpen }"
        @click="memoOpen = !memoOpen"
        :title="memoOpen ? '메모 닫기' : '메모 열기'"
      >
        <StickyNote :size="18" />
        <span v-if="!sidebarCollapsed" class="memo-toggle-label">메모장</span>
        <el-tooltip v-if="sidebarCollapsed" content="메모장" placement="right">
          <span></span>
        </el-tooltip>
      </button>

      <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <ChevronLeft v-if="!sidebarCollapsed" :size="18" />
        <ChevronRight v-else :size="18" />
      </button>
    </aside>

    <!-- 메모 패널 -->
    <MemoPanel
      :open="memoOpen"
      :sidebar-collapsed="sidebarCollapsed"
      @close="memoOpen = false"
    />

    <!-- 채팅 패널 -->
    <ChatPanel
      :open="chatOpen"
      @close="chatOpen = false"
    />

    <!-- 메인 콘텐츠 -->
    <div class="app-main" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <!-- 헤더 -->
      <header class="app-header">
        <div class="header-left">
          <div class="breadcrumb">
            <span class="bc-root">Workoop</span>
            <ChevronRight :size="13" class="bc-sep" />
            <span class="bc-current">{{ currentPageTitle }}</span>
          </div>
        </div>
        <div class="header-right">
          <span class="today-date">{{ todayString }}</span>
          <div v-if="orgStore.currentOrg" class="org-badge">
            <Building2 :size="13" />
            {{ orgStore.currentOrg.name }}
          </div>
          <button class="notif-trigger" @click="notifPanelOpen = true" title="알림">
            <Bell :size="16" />
            <span v-if="notifStore.unreadCount > 0" class="notif-badge">
              {{ notifStore.unreadCount > 9 ? '9+' : notifStore.unreadCount }}
            </span>
          </button>
          <button class="gs-trigger" @click="openGlobalSearch" title="글로벌 검색 (⌘K)">
            <Search :size="16" />
          </button>
          <button class="dark-toggle" @click="toggleDark" :title="isDark ? '라이트 모드' : '다크 모드'">
            <Moon v-if="!isDark" :size="16" />
            <Sun v-else :size="16" />
          </button>
          <div class="user-avatar" @click="profileModalOpen = true" :title="`${authStore.user?.name} 내 정보`">
            {{ userInitial }}
          </div>
        </div>
      </header>

      <!-- 뷰 영역 -->
      <main class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="slide-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 알림 패널 -->
    <NotificationPanel :open="notifPanelOpen" @close="notifPanelOpen = false" />

    <!-- 내 정보 모달 -->
    <div v-if="profileModalOpen" class="profile-modal-overlay" @click.self="profileModalOpen = false">
      <div class="profile-modal">
        <button class="profile-close" @click="profileModalOpen = false" title="닫기">×</button>

        <div class="profile-head">
          <div class="profile-avatar-lg">{{ userInitial }}</div>
          <div class="profile-name">{{ authStore.user?.name || '이름 없음' }}</div>
          <div class="profile-email">{{ authStore.user?.email || '' }}</div>
        </div>

        <div class="profile-section">
          <div class="profile-section-title">소속 조직</div>
          <div v-if="orgStore.orgs.length === 0" class="profile-empty">가입된 조직이 없습니다</div>
          <div v-else class="profile-org-list">
            <div
              v-for="o in orgStore.orgs"
              :key="o.id"
              class="profile-org-item"
              :class="{ active: o.id === orgStore.currentOrg?.id }"
              :title="o.id === orgStore.currentOrg?.id ? '현재 보고 있는 조직' : '클릭하여 이 조직으로 전환'"
              @click="switchOrg(o)"
            >
              <div class="profile-org-info">
                <div class="profile-org-name">{{ o.name }}</div>
                <div class="profile-org-slug">@{{ o.slug }}</div>
              </div>
              <span class="profile-org-role" :class="`role-${(o.role || 'MEMBER').toLowerCase()}`">
                {{ o.role || 'MEMBER' }}
              </span>
              <span v-if="o.id === orgStore.currentOrg?.id" class="profile-org-current">현재</span>
            </div>
          </div>
        </div>

        <div class="profile-section">
          <div class="profile-section-title">계정 정보</div>
          <div class="profile-info-rows">
            <div class="profile-info-row">
              <span class="profile-info-label">사용자 ID</span>
              <span class="profile-info-val">#{{ authStore.user?.id }}</span>
            </div>
            <div class="profile-info-row">
              <span class="profile-info-label">이메일</span>
              <span class="profile-info-val">{{ authStore.user?.email }}</span>
            </div>
          </div>
        </div>

        <div class="profile-actions">
          <button class="profile-btn-secondary" @click="goToSettings">설정 페이지로 이동</button>
          <button class="profile-btn-logout" @click="handleLogout">로그아웃</button>
        </div>
      </div>
    </div>

    <!-- 채팅 FAB -->
    <transition name="fab-pop">
      <button
        v-if="!chatOpen"
        class="chat-fab"
        @click="toggleChat"
        title="팀 채팅 (C)"
      >
        <span v-if="chatStore.unread > 0" class="fab-pulse-ring" />
        <MessageSquare :size="22" />
        <span v-if="chatStore.unread > 0" class="fab-badge">
          {{ chatStore.unread > 9 ? '9+' : chatStore.unread }}
        </span>
      </button>
    </transition>

    <!-- 집중 모드 -->
    <FocusMode />

    <!-- 글로벌 검색 모달 (Cmd+K) -->
    <div v-if="globalSearchOpen" class="gs-overlay" @click.self="closeSearch">
      <div class="gs-modal">
        <div class="gs-input-wrap">
          <Search :size="18" class="gs-search-icon" />
          <input
            ref="gsInputRef"
            v-model="gsQuery"
            class="gs-input"
            placeholder="페이지, KPI 이름, 카테고리 검색..."
            @keydown.escape="closeSearch"
            @keydown="onGsKeydown"
          />
          <span class="gs-kbd">ESC</span>
        </div>
        <div class="gs-results" v-if="gsResults.length > 0">
          <div
            v-for="(item, idx) in gsResults"
            :key="item.path + idx"
            class="gs-result-item"
            :class="{ 'gs-result-active': idx === gsSelectedIdx }"
            @click="goGsItem(item)"
            @mousemove="gsSelectedIdx = idx"
          >
            <component :is="item.icon" :size="16" class="gs-item-icon" />
            <div>
              <div class="gs-item-name">{{ item.name }}</div>
              <div class="gs-item-sub" v-if="item.sub">{{ item.sub }}</div>
            </div>
          </div>
        </div>
        <div v-else-if="gsQuery" class="gs-no-result">검색 결과가 없습니다.</div>
        <div class="gs-footer">
          <span class="gs-hint"><kbd>↑↓</kbd> 이동</span>
          <span class="gs-hint"><kbd>Enter</kbd> 선택</span>
          <span class="gs-hint"><kbd>Esc</kbd> 닫기</span>
          <span class="gs-hint" style="margin-left:auto"><kbd>⌘K</kbd> 언제든지</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'
import {
  LayoutDashboard, BarChart3, ListChecks, Tag, ChevronLeft, ChevronRight, Settings,
  LineChart, CalendarDays, BookOpen, Moon, Sun, Search, ClipboardList, StickyNote, Star,
  Building2, Kanban, Headphones, Bell, BarChart2, MessageSquare
} from 'lucide-vue-next'
import { useKpiStore } from '@/store/kpiStore'
import { useAuthStore } from '@/store/authStore'
import { useOrgStore } from '@/store/orgStore'
import { useRouter } from 'vue-router'
import { orgApi } from '@/api/orgApi'
import MemoPanel from '@/components/layout/MemoPanel.vue'
import ChatPanel from '@/components/layout/ChatPanel.vue'
import FocusMode from '@/components/FocusMode.vue'
import NotificationPanel from '@/components/NotificationPanel.vue'
import { useFocusStore } from '@/store/focusStore'
import { useNotificationStore } from '@/store/notificationStore'
import { useWsStore } from '@/store/wsStore'
import { useChatStore } from '@/store/chatStore'

dayjs.locale('ko')
const router    = useRouter()
const authStore = useAuthStore()
const orgStore  = useOrgStore()

const userInitial = computed(() =>
  authStore.user?.name?.charAt(0)?.toUpperCase() || '?'
)

const focusStore  = useFocusStore()
const notifStore  = useNotificationStore()
const wsStore     = useWsStore()
const chatStore   = useChatStore()
const notifPanelOpen = ref(false)
const profileModalOpen = ref(false)

function goToSettings() {
  profileModalOpen.value = false
  router.push('/settings')
}

function switchOrg(org) {
  if (!org || org.id === orgStore.currentOrg?.id) {
    profileModalOpen.value = false
    return
  }
  orgStore.setCurrentOrg(org)
  // stale data 방지를 위해 전체 리로드 (모든 store/WS가 새 orgId로 재초기화됨)
  window.location.href = '/dashboard'
}
const chatOpen    = ref(false)

function toggleChat() {
  chatOpen.value = !chatOpen.value
  if (chatOpen.value) chatStore.markRead()
}
const teamMembers = ref([])
async function loadTeamMembers() {
  if (!orgStore.currentOrg) return
  try {
    const res = await orgApi.members(orgStore.currentOrg.id)
    teamMembers.value = (res.data || []).filter(m => m.userId !== authStore.user?.id)
  } catch {}
}

function handleLogout() {
  if (confirm('로그아웃 하시겠습니까?')) {
    wsStore.disconnect()
    notifStore.stopPolling()
    authStore.logout()
    orgStore.clear()
    router.push('/login')
  }
}

// 레이아웃 레벨 WS 핸들러 unsubscribe 추적 (중복 등록 방지)
const _wsLayoutUnsubs = []

function _initWs() {
  const token = authStore.token
  const orgId = orgStore.currentOrg?.id
  if (!token || !orgId) return

  // 기존 핸들러 제거 후 재등록 (중복 방지)
  _wsLayoutUnsubs.forEach(fn => fn())
  _wsLayoutUnsubs.length = 0

  wsStore.connect(token, orgId)
  _wsLayoutUnsubs.push(
    wsStore.on('NOTIFICATION', e => notifStore.handleWsNotification(e)),
    wsStore.on('CHAT_MESSAGE', e => {
      chatStore.appendMessage(e.payload)
      if (!chatOpen.value) chatStore.incrementUnread()
    })
  )
}

const sidebarCollapsed = ref(false)
const memoOpen = ref(false)
const route = useRoute()

const isDark = ref(localStorage.getItem('workoop-dark') === 'true')
function toggleDark() {
  isDark.value = !isDark.value
  localStorage.setItem('workoop-dark', isDark.value)
  applyDark()
}
function applyDark() {
  document.documentElement.classList.toggle('dark', isDark.value)
}

const navGroups = [
  {
    label: '업무',
    items: [
      { path: '/dashboard',      label: '대시보드',     icon: LayoutDashboard },
      { path: '/team-dashboard', label: '팀 대시보드',  icon: BarChart2 },
      { path: '/projects',       label: '프로젝트',      icon: Kanban },
      { path: '/scrum',          label: '데일리 스크럼', icon: ClipboardList },
    ]
  },
  {
    label: '성과',
    items: [
      { path: '/kpis',      label: 'KPI 관리',  icon: BarChart3 },
      { path: '/records',   label: '실적 입력', icon: ListChecks },
      { path: '/analytics', label: '분석',       icon: LineChart },
    ]
  },
  {
    label: '기록',
    items: [
      { path: '/star',     label: 'STAR 노트', icon: Star },
      { path: '/calendar', label: '캘린더',    icon: CalendarDays },
      { path: '/review',   label: '회고',       icon: BookOpen },
    ]
  },
  {
    label: '관리',
    items: [
      { path: '/categories', label: '카테고리', icon: Tag },
      { path: '/settings',   label: '설정',      icon: Settings },
    ]
  }
]

const navItems = navGroups.flatMap(g => g.items)

const currentPageTitle = computed(() => {
  const sorted = [...navItems].sort((a, b) => b.path.length - a.path.length)
  const item = sorted.find(n => route.path.startsWith(n.path))
  return item?.label ?? 'Workoop'
})

const todayString = computed(() =>
  dayjs().format('YYYY년 MM월 DD일 dddd')
)

// ── 글로벌 검색 ──
const store = useKpiStore()
const globalSearchOpen = ref(false)
const gsQuery          = ref('')
const gsInputRef       = ref()
const gsSelectedIdx    = ref(0)

const gsResults = computed(() => {
  const q = gsQuery.value.toLowerCase().trim()
  const catMap = {}
  ;(store.categories || []).forEach(c => { catMap[c.id] = c.name })

  if (!q) {
    return navItems.map(n => ({ name: n.label, path: n.path, icon: n.icon, sub: n.path }))
  }

  const results = []
  navItems.forEach(n => {
    if (n.label.toLowerCase().includes(q)) {
      results.push({ name: n.label, path: n.path, icon: n.icon, sub: n.path })
    }
  })
  ;(store.kpis || []).forEach(k => {
    const catName = catMap[k.categoryId] || ''
    if (k.name.toLowerCase().includes(q) || catName.toLowerCase().includes(q)) {
      results.push({ name: k.name, path: `/kpis/${k.id}`, icon: BarChart3, sub: catName })
    }
  })

  return results.slice(0, 8)
})

watch(gsQuery, () => { gsSelectedIdx.value = 0 })

function openGlobalSearch() {
  globalSearchOpen.value = true
  gsQuery.value          = ''
  gsSelectedIdx.value    = 0
  nextTick(() => gsInputRef.value?.focus())
}

function closeSearch() {
  globalSearchOpen.value = false
}

function goGsItem(item) {
  router.push(item.path)
  closeSearch()
}

function onGsKeydown(e) {
  const len = gsResults.value.length
  if (!len) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    gsSelectedIdx.value = (gsSelectedIdx.value + 1) % len
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    gsSelectedIdx.value = (gsSelectedIdx.value - 1 + len) % len
  } else if (e.key === 'Enter') {
    e.preventDefault()
    goGsItem(gsResults.value[gsSelectedIdx.value])
  }
}

onUnmounted(() => {
  _wsLayoutUnsubs.forEach(fn => fn())
  wsStore.disconnect()
  notifStore.stopPolling()
})

onMounted(async () => {
  applyDark()
  // 조직 목록을 가장 먼저 로드 (KPI/팀멤버/WS 모두 currentOrg에 의존)
  try {
    await orgStore.fetchOrgs()
  } catch (e) {
    console.warn('[AppLayout] 조직 목록 로드 실패', e)
  }
  store.fetchKpis()
  store.fetchCategories()
  loadTeamMembers()
  notifStore.fetch()              // 초기 알림 목록 즉시 로드
  notifStore.startPolling()       // WS 끊김 대비 fallback 폴링
  _initWs()                       // WebSocket 연결
  window.addEventListener('keydown', (e) => {
    const tag = document.activeElement?.tagName
    const isInput = ['INPUT','TEXTAREA','SELECT'].includes(tag) || document.activeElement?.isContentEditable
    if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
      e.preventDefault()
      openGlobalSearch()
    }
    if (e.key === 'f' && !isInput && !e.metaKey && !e.ctrlKey) {
      e.preventDefault()
      if (focusStore.active) focusStore.exit()
      else focusStore.enter()
    }
    if (e.key === 'c' && !isInput && !e.metaKey && !e.ctrlKey) {
      e.preventDefault()
      toggleChat()
    }
    if (e.key === 'Escape' && focusStore.active) {
      focusStore.exit()
    }
  })
})
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ── 사이드바 — Asana 다크 네브 ── */
.app-sidebar {
  width: 260px;
  min-width: 260px;
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease, min-width 0.2s ease;
  position: relative;
  z-index: 100;
}
.app-sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

/* 로고 */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 14px;
  border-bottom: 1px solid rgba(255,255,255,0.07);
  text-decoration: none;
  transition: background var(--transition-base);
  flex-shrink: 0;
}
.sidebar-logo:hover { background: rgba(255,255,255,0.04); }

.logo-icon {
  width: 32px; height: 32px;
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.logo-icon img {
  width: 32px; height: 32px;
  border-radius: 7px;
  object-fit: contain;
}

.logo-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
  min-width: 0;
}
.logo-text {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.4px;
  white-space: nowrap;
  line-height: 1.2;
}
.logo-org {
  font-size: 11px;
  color: rgba(255,255,255,0.38);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

/* 네비게이션 */
.sidebar-nav {
  flex: 1;
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  gap: 0;
}

.nav-group-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255,255,255,0.30);
  padding: 12px 8px 4px;
  display: block;
  user-select: none;
}

.nav-group-divider {
  height: 1px;
  background: rgba(255,255,255,0.06);
  margin: 8px 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  color: rgba(255,255,255,0.60);
  text-decoration: none;
  transition: background var(--transition-fast), color var(--transition-fast);
  white-space: nowrap;
  overflow: hidden;
  margin-bottom: 1px;
  font-family: inherit;
}
.nav-item:hover {
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.90);
}
.nav-item--active {
  background: rgba(255,255,255,0.10);
  color: #fff;
  font-weight: 600;
}
.nav-label {
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}
.nav-item--active .nav-label { font-weight: 600; }

/* 팀 멤버 */
.sidebar-members {
  display: flex; align-items: center;
  padding: 6px 12px; margin: 0 4px 2px;
}
.sidebar-member-avatar {
  width: 24px; height: 24px; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-project), var(--color-focus));
  color: white; font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--bg-sidebar); margin-left: -5px;
  cursor: default; flex-shrink: 0;
}
.sidebar-member-avatar:first-child { margin-left: 0; }
.sidebar-member-more {
  width: 24px; height: 24px; border-radius: 50%;
  background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.45);
  font-size: 9px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--bg-sidebar); margin-left: -5px;
}

/* 집중/메모 토글 */
.focus-toggle,
.memo-toggle {
  display: flex; align-items: center; gap: 10px;
  margin: 0 8px 1px; padding: 8px 10px;
  border: none; border-radius: 6px;
  background: transparent; color: rgba(255,255,255,0.38);
  cursor: pointer; font-size: 14px; font-weight: 500;
  white-space: nowrap; overflow: hidden;
  transition: background var(--transition-fast), color var(--transition-fast);
  width: calc(100% - 16px); text-align: left;
  font-family: inherit;
}
.focus-toggle:hover,
.memo-toggle:hover {
  background: rgba(255,255,255,0.06);
  color: rgba(255,255,255,0.80);
}

.focus-toggle.active {
  background: rgba(48,127,226,0.15);
  color: #93C5FD;
  animation: focus-pulse 2.5s ease-in-out infinite;
}
@keyframes focus-pulse {
  0%, 100% { background: rgba(48,127,226,0.12); }
  50%       { background: rgba(48,127,226,0.22); }
}
.focus-toggle-label,
.memo-toggle-label { font-size: 14px; font-weight: 500; }

.memo-toggle.active { background: rgba(234,179,8,0.12); color: #FCD34D; }

/* 채팅 토글 */
.chat-toggle {
  display: flex; align-items: center; gap: 10px;
  margin: 0 8px 1px; padding: 8px 10px;
  border: none; border-radius: 6px;
  background: transparent; color: rgba(255,255,255,0.38);
  cursor: pointer; font-size: 14px; font-weight: 500;
  white-space: nowrap; overflow: hidden;
  transition: background var(--transition-fast), color var(--transition-fast);
  width: calc(100% - 16px); text-align: left;
  font-family: inherit;
  position: relative;
}
.chat-toggle:hover { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.80); }
.chat-toggle.active { background: rgba(99,102,241,0.15); color: #A5B4FC; }
.chat-toggle-label { font-size: 14px; font-weight: 500; flex: 1; }
.chat-unread-badge {
  background: #EF4444; color: white;
  font-size: 9px; font-weight: 800;
  min-width: 16px; height: 16px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 4px;
}

/* 사이드바 접기 */
.sidebar-toggle {
  margin: 8px;
  padding: 7px;
  border: none;
  background: rgba(255,255,255,0.04);
  border-radius: 6px;
  color: rgba(255,255,255,0.30);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}
.sidebar-toggle:hover {
  background: rgba(255,255,255,0.08);
  color: rgba(255,255,255,0.65);
}

/* ── 메인 ── */
.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 헤더 — Asana 화이트 탑바 ── */
.app-header {
  height: 52px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

/* 브레드크럼 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 5px;
}
.bc-root {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}
.bc-sep {
  color: var(--gray-300);
  flex-shrink: 0;
}
.bc-current {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.today-date {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dark-toggle {
  width: 30px; height: 30px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted);
  transition: background var(--transition-fast), color var(--transition-fast);
}
.dark-toggle:hover { background: var(--bg-hover); color: var(--text-primary); }

.notif-trigger {
  position: relative; width: 30px; height: 30px;
  border: 1px solid var(--border-color); border-radius: 6px;
  background: transparent; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted);
  transition: background var(--transition-fast), color var(--transition-fast);
}
.notif-trigger:hover { background: var(--bg-hover); color: var(--text-primary); }
.notif-badge {
  position: absolute; top: -3px; right: -3px;
  background: var(--color-danger); color: white;
  font-size: 9px; font-weight: 800; min-width: 15px; height: 15px;
  border-radius: 8px; display: flex; align-items: center; justify-content: center;
  padding: 0 3px; border: 2px solid var(--bg-card);
}

.org-badge {
  display: flex; align-items: center; gap: 4px;
  padding: 3px 9px; background: var(--bg-hover);
  border-radius: 6px; font-size: 12px; font-weight: 600;
  color: var(--text-secondary); border: 1px solid var(--border-color);
}

.user-avatar {
  width: 30px; height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-project), var(--color-focus));
  color: white; font-size: 13px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0;
  transition: opacity var(--transition-fast);
}
.user-avatar:hover { opacity: 0.80; }

/* 글로벌 검색 트리거 */
.gs-trigger {
  width: 30px; height: 30px;
  border: none; border-radius: 6px;
  background: transparent; color: var(--text-muted);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.gs-trigger:hover { background: var(--bg-hover); color: var(--text-primary); }

/* ── 글로벌 검색 모달 ── */
.gs-overlay {
  position: fixed; inset: 0; background: rgba(17,24,39,0.45);
  z-index: 9999; display: flex; align-items: flex-start; justify-content: center;
  padding-top: 8vh;
  backdrop-filter: blur(3px);
  animation: gs-overlay-in 0.12s ease;
}
@keyframes gs-overlay-in { from { opacity: 0; } to { opacity: 1; } }

.gs-modal {
  width: 540px; max-width: 92vw;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl), 0 0 0 1px var(--border-color);
  overflow: hidden;
  animation: gs-modal-in 0.18s cubic-bezier(0.22, 0.61, 0.36, 1);
}
@keyframes gs-modal-in {
  from { opacity: 0; transform: translateY(-16px) scale(0.96); }
  to   { opacity: 1; transform: none; }
}

.gs-input-wrap {
  display: flex; align-items: center; gap: 10px;
  padding: 13px 16px; border-bottom: 1px solid var(--border-color);
}
.gs-search-icon { color: var(--text-muted); flex-shrink: 0; }
.gs-input {
  flex: 1; border: none; outline: none; font-size: 15px;
  color: var(--text-primary); background: transparent;
  font-family: inherit; font-weight: 400;
}
.gs-kbd {
  font-size: 11px; background: var(--bg-hover); color: var(--text-muted);
  padding: 2px 7px; border-radius: 4px; font-family: inherit; white-space: nowrap;
  border: 1px solid var(--border-color);
}

.gs-results { max-height: 280px; overflow-y: auto; padding: 6px; }
.gs-result-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 11px; border-radius: 6px; cursor: pointer;
  transition: background var(--transition-fast);
}
.gs-result-item:hover { background: var(--bg-hover); }
.gs-result-active { background: rgba(48,127,226,0.08) !important; }
.gs-result-active .gs-item-icon { color: var(--color-project); }
.gs-result-active .gs-item-name { color: var(--color-project); }
.gs-item-icon { color: var(--text-secondary); flex-shrink: 0; }
.gs-item-name { font-size: 14px; font-weight: 600; color: var(--text-primary); line-height: 1.3; }
.gs-item-sub  { font-size: 12px; color: var(--text-muted); margin-top: 1px; }

.gs-no-result { padding: 28px; text-align: center; font-size: 13px; color: var(--text-muted); }

.gs-footer {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 14px; background: var(--bg-hover); border-top: 1px solid var(--border-color);
}
.gs-hint { font-size: 11px; color: var(--text-muted); }
.gs-hint kbd {
  background: var(--bg-card); color: var(--text-secondary);
  padding: 1px 5px; border-radius: 3px; font-size: 11px; font-family: inherit;
  border: 1px solid var(--border-color);
}

/* ── 콘텐츠 ── */
.app-content {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-main);
}

/* ── 채팅 FAB ── */
.chat-fab {
  position: fixed;
  bottom: 28px;
  right: 28px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(79,70,229,0.45), 0 2px 8px rgba(0,0,0,0.25);
  z-index: 140;
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.2s ease;
}
.chat-fab:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 28px rgba(79,70,229,0.6), 0 2px 12px rgba(0,0,0,0.3);
}
.chat-fab:active {
  transform: scale(0.95);
}

/* 미읽음 펄스 링 */
.fab-pulse-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(79,70,229,0.6);
  animation: fab-pulse 1.8s ease-out infinite;
  pointer-events: none;
}
@keyframes fab-pulse {
  0%   { transform: scale(1);    opacity: 0.8; }
  70%  { transform: scale(1.35); opacity: 0;   }
  100% { transform: scale(1.35); opacity: 0;   }
}

/* 미읽음 뱃지 */
.fab-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #EF4444;
  color: white;
  font-size: 9px;
  font-weight: 800;
  min-width: 17px;
  height: 17px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  border: 2px solid var(--bg-main, #F1F5F9);
}

/* FAB 등장/퇴장 애니메이션 */
.fab-pop-enter-active {
  transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.fab-pop-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fab-pop-enter-from {
  opacity: 0;
  transform: scale(0.5);
}
.fab-pop-leave-to {
  opacity: 0;
  transform: scale(0.7);
}

/* ============ 내 정보 모달 ============ */
.profile-modal-overlay {
  position: fixed; inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999;
  animation: profile-fade 0.18s ease-out;
}
@keyframes profile-fade { from { opacity: 0 } to { opacity: 1 } }

.profile-modal {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
  width: 420px; max-width: calc(100vw - 32px);
  max-height: calc(100vh - 64px);
  overflow-y: auto;
  padding: 28px 24px 20px;
  animation: profile-slide 0.22s ease-out;
}
@keyframes profile-slide {
  from { opacity: 0; transform: translateY(-12px) scale(0.98) }
  to { opacity: 1; transform: translateY(0) scale(1) }
}

.profile-close {
  position: absolute; top: 10px; right: 12px;
  width: 28px; height: 28px; border-radius: 50%;
  border: none; background: transparent;
  font-size: 1.4rem; line-height: 1; color: var(--text-muted);
  cursor: pointer; transition: all var(--transition-fast);
}
.profile-close:hover { background: var(--bg-hover); color: var(--text-primary); }

.profile-head { text-align: center; padding: 4px 0 18px; }
.profile-avatar-lg {
  width: 64px; height: 64px;
  border-radius: 50%;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #6366F1, #8B5CF6);
  color: white;
  font-size: 1.6rem; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.35);
}
.profile-name { font-size: 1.15rem; font-weight: 800; color: var(--text-primary); }
.profile-email { font-size: 0.85rem; color: var(--text-muted); margin-top: 2px; }

.profile-section {
  border-top: 1px solid var(--border-color);
  padding: 16px 0 4px;
}
.profile-section-title {
  font-size: 0.72rem; font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase; letter-spacing: 0.04em;
  margin-bottom: 10px;
}

.profile-org-list { display: flex; flex-direction: column; gap: 8px; }
.profile-org-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--bg-app);
  transition: all var(--transition-fast);
  cursor: pointer;
}
.profile-org-item:hover {
  border-color: var(--color-project);
  background: var(--bg-hover);
  transform: translateY(-1px);
}
.profile-org-item.active {
  border-color: var(--color-project);
  background: rgba(79, 70, 229, 0.05);
  cursor: default;
}
.profile-org-item.active:hover {
  transform: none;
  background: rgba(79, 70, 229, 0.05);
}
.profile-org-info { flex: 1; min-width: 0; }
.profile-org-name {
  font-size: 0.92rem; font-weight: 700; color: var(--text-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.profile-org-slug { font-size: 0.74rem; color: var(--text-muted); margin-top: 1px; }
.profile-org-role {
  font-size: 0.68rem; font-weight: 700;
  padding: 3px 8px; border-radius: 99px;
  letter-spacing: 0.03em;
}
.role-owner { background: rgba(234, 88, 12, 0.12); color: #C2410C; }
.role-admin { background: rgba(99, 102, 241, 0.12); color: #4F46E5; }
.role-member { background: var(--bg-hover); color: var(--text-secondary); }
.profile-org-current {
  font-size: 0.68rem; font-weight: 700;
  color: var(--color-project);
  background: rgba(79, 70, 229, 0.1);
  padding: 3px 8px; border-radius: 99px;
}
.profile-empty {
  text-align: center; font-size: 0.85rem;
  color: var(--text-muted); padding: 12px 0;
}

.profile-info-rows { display: flex; flex-direction: column; gap: 8px; }
.profile-info-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 0.85rem;
  padding: 8px 12px;
  background: var(--bg-app);
  border-radius: 8px;
}
.profile-info-label { color: var(--text-secondary); }
.profile-info-val { color: var(--text-primary); font-weight: 600; font-family: 'JetBrains Mono', monospace; font-size: 0.82rem; }

.profile-actions {
  display: flex; gap: 8px;
  padding-top: 18px; margin-top: 4px;
  border-top: 1px solid var(--border-color);
}
.profile-btn-secondary, .profile-btn-logout {
  flex: 1;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 0.88rem; font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid var(--border-color);
}
.profile-btn-secondary { background: var(--bg-card); color: var(--text-primary); }
.profile-btn-secondary:hover { background: var(--bg-hover); border-color: var(--color-project); color: var(--color-project); }
.profile-btn-logout { background: var(--color-danger); color: white; border-color: var(--color-danger); }
.profile-btn-logout:hover { filter: brightness(1.08); }
</style>
