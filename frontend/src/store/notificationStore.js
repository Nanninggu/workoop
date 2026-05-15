import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationApi } from '@/api/notificationApi'

// WS가 끊겼을 때의 안전망 폴링 간격 (5분)
const FALLBACK_POLL_MS = 300_000

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const loading       = ref(false)
  let   _pollTimer    = null

  const unreadCount = computed(() =>
    notifications.value.filter(n => !n.readAt).length
  )

  const unread = computed(() =>
    notifications.value.filter(n => !n.readAt)
  )

  async function fetch() {
    try {
      const res = await notificationApi.list()
      notifications.value = res.data || []
    } catch {}
  }

  async function markRead(id) {
    await notificationApi.markRead(id)
    const n = notifications.value.find(n => n.id === id)
    if (n) n.readAt = new Date().toISOString()
  }

  async function markAllRead() {
    await notificationApi.markAllRead()
    notifications.value.forEach(n => {
      if (!n.readAt) n.readAt = new Date().toISOString()
    })
  }

  // WebSocket에서 서버 push 수신 시 호출
  function handleWsNotification(event) {
    const notif = event.payload
    if (!notif) return
    // 중복 방지: 같은 id가 이미 있으면 갱신, 없으면 맨 앞에 추가
    const idx = notifications.value.findIndex(n => n.id === notif.id)
    if (idx > -1) {
      notifications.value[idx] = notif
    } else {
      notifications.value.unshift(notif)
    }
  }

  // WS가 끊겼을 때 대비한 fallback 폴링 (5분 간격)
  function startFallbackPolling() {
    stopPolling()
    fetch()
    _pollTimer = setInterval(fetch, FALLBACK_POLL_MS)
  }

  function stopPolling() {
    clearInterval(_pollTimer)
    _pollTimer = null
  }

  // 이전 호환성 유지 (AppLayout 등에서 startPolling() 호출)
  const startPolling = startFallbackPolling

  function typeIcon(type) {
    const map = {
      TASK_ASSIGNED: '📋',
      TASK_DONE:     '✅',
      KPI_ACHIEVED:  '🎯',
      BLOCKER_HIGH:  '🚨',
    }
    return map[type] || '🔔'
  }

  function typeColor(type) {
    const map = {
      TASK_ASSIGNED: '#3B82F6',
      TASK_DONE:     '#10B981',
      KPI_ACHIEVED:  '#F59E0B',
      BLOCKER_HIGH:  '#EF4444',
    }
    return map[type] || '#94A3B8'
  }

  return {
    notifications, loading, unreadCount, unread,
    fetch, markRead, markAllRead,
    handleWsNotification,
    startPolling, startFallbackPolling, stopPolling,
    typeIcon, typeColor,
  }
})
