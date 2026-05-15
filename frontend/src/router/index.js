import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login',      name: 'Login',      component: () => import('@/views/LoginView.vue'),      meta: { public: true,  title: '로그인' } },
  { path: '/signup',     name: 'Signup',     component: () => import('@/views/SignupView.vue'),     meta: { public: true,  title: '회원가입' } },
  { path: '/onboarding', name: 'Onboarding', component: () => import('@/views/OnboardingView.vue'), meta: { public: false, title: '조직 설정' } },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { public: false },
    children: [
      { path: '',           redirect: '/dashboard' },
      { path: 'dashboard',       name: 'Dashboard',       component: () => import('@/views/DashboardView.vue'),       meta: { title: '대시보드' } },
      { path: 'team-dashboard',  name: 'TeamDashboard',   component: () => import('@/views/TeamDashboardView.vue'),   meta: { title: '팀 대시보드' } },
      { path: 'projects',   name: 'Projects',   component: () => import('@/views/ProjectListView.vue'), meta: { title: '프로젝트' } },
      { path: 'projects/:id/board', name: 'Kanban', component: () => import('@/views/KanbanView.vue'), meta: { title: '칸반 보드' } },
      { path: 'kpis',       name: 'KpiList',    component: () => import('@/views/KpiListView.vue'),    meta: { title: 'KPI 관리' } },
      { path: 'kpis/:id',   name: 'KpiDetail',  component: () => import('@/views/KpiDetailView.vue'),  meta: { title: 'KPI 상세' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/CategoryView.vue'),   meta: { title: '카테고리 관리' } },
      { path: 'records',    name: 'Records',    component: () => import('@/views/RecordView.vue'),     meta: { title: '실적 입력' } },
      { path: 'scrum',      name: 'DailyScrum', component: () => import('@/views/DailyScrumView.vue'), meta: { title: '데일리 스크럼' } },
      { path: 'analytics',  name: 'Analytics',  component: () => import('@/views/AnalyticsView.vue'),  meta: { title: '분석' } },
      { path: 'calendar',   name: 'Calendar',   component: () => import('@/views/CalendarView.vue'),   meta: { title: '캘린더' } },
      { path: 'review',     name: 'Review',     component: () => import('@/views/ReviewView.vue'),     meta: { title: '회고' } },
      { path: 'star',       name: 'Star',       component: () => import('@/views/StarView.vue'),       meta: { title: 'STAR 노트' } },
      { path: 'settings',   name: 'Settings',   component: () => import('@/views/SettingsView.vue'),   meta: { title: '설정' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} | Workoop` : 'Workoop'

  const token = localStorage.getItem('coop-token')
  const isPublic = to.meta.public === true

  if (!isPublic && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (isPublic && token && (to.path === '/login' || to.path === '/signup')) {
    return { path: '/dashboard' }
  }
})

export default router
