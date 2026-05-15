import { defineStore } from 'pinia'
import { ref } from 'vue'
import { chatApi } from '@/api/chatApi'

export const useChatStore = defineStore('chat', () => {
  const messages  = ref([])
  const loading   = ref(false)
  const unread    = ref(0)

  async function fetchHistory(orgId) {
    loading.value = true
    try {
      const res = await chatApi.getHistory(orgId)
      messages.value = res.data || []
    } catch {}
    loading.value = false
  }

  async function sendMessage(orgId, content) {
    await chatApi.send(orgId, content)
    // 메시지는 WS 브로드캐스트로 받아서 append (낙관적 추가 불필요)
  }

  function appendMessage(msg) {
    messages.value.push(msg)
  }

  function markRead() {
    unread.value = 0
  }

  function incrementUnread() {
    unread.value++
  }

  return { messages, loading, unread, fetchHistory, sendMessage, appendMessage, markRead, incrementUnread }
})
