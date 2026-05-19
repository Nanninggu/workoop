import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

// 현재 호스트 기준 — Vite dev(51300)에서는 프록시로, 48080 정적 서빙/EC2에서는 같은 origin으로 동작
const WS_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`

export const useWsStore = defineStore('ws', () => {
  const connected   = ref(false)
  const _client     = ref(null)
  // Map<eventType, handler[]>
  const _handlers   = new Map()

  // ── 이벤트 구독 ─────────────────────────────────────────────────────────────
  // on('TASK_CREATED', handler)  or  on('*', handler) for all events
  // returns unsubscribe function
  function on(type, handler) {
    if (!_handlers.has(type)) _handlers.set(type, [])
    _handlers.get(type).push(handler)
    return () => {
      const arr = _handlers.get(type)
      if (arr) {
        const idx = arr.indexOf(handler)
        if (idx > -1) arr.splice(idx, 1)
      }
    }
  }

  function _emit(type, event) {
    _handlers.get(type)?.forEach(h => h(event))
    if (type !== '*') _handlers.get('*')?.forEach(h => h(event))
  }

  // ── 연결 ────────────────────────────────────────────────────────────────────
  function connect(token, orgId) {
    if (_client.value?.active) return

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true

        client.subscribe(`/topic/org/${orgId}/tasks`, msg => {
          _emit(JSON.parse(msg.body).type, JSON.parse(msg.body))
        })
        client.subscribe(`/topic/org/${orgId}/scrums`, msg => {
          _emit(JSON.parse(msg.body).type, JSON.parse(msg.body))
        })
        client.subscribe(`/topic/org/${orgId}/projects`, msg => {
          _emit(JSON.parse(msg.body).type, JSON.parse(msg.body))
        })
        client.subscribe(`/topic/org/${orgId}/chat`, msg => {
          _emit(JSON.parse(msg.body).type, JSON.parse(msg.body))
        })
        client.subscribe(`/user/queue/notifications`, msg => {
          _emit('NOTIFICATION', JSON.parse(msg.body))
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: frame => {
        console.error('[WS] STOMP error', frame.headers?.message)
      },
      onWebSocketError: (e) => {
        connected.value = false
        console.error('[WS] WebSocket 오류', e)
      },
    })

    client.activate()
    _client.value = client
  }

  function disconnect() {
    _client.value?.deactivate()
    _client.value = null
    connected.value = false
  }

  // 조직 변경 시 재연결
  function reconnect(token, orgId) {
    disconnect()
    connect(token, orgId)
  }

  return { connected, connect, disconnect, reconnect, on }
})
