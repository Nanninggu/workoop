import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/authApi'
import { ElMessage } from 'element-plus'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('coop-token') || null)
  const user  = ref(JSON.parse(localStorage.getItem('coop-user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  function _persist(t, u) {
    token.value = t
    user.value  = u
    localStorage.setItem('coop-token', t)
    localStorage.setItem('coop-user', JSON.stringify(u))
  }

  async function login(email, password) {
    const res = await authApi.login({ email, password })
    _persist(res.data.token, res.data.user)
    ElMessage.success(`${res.data.user.name}님, 환영합니다!`)
    return res.data
  }

  async function signup(email, password, name) {
    const res = await authApi.signup({ email, password, name })
    _persist(res.data.token, res.data.user)
    ElMessage.success('회원가입이 완료되었습니다.')
    return res.data
  }

  function logout() {
    token.value = null
    user.value  = null
    localStorage.removeItem('coop-token')
    localStorage.removeItem('coop-user')
    localStorage.removeItem('coop-current-org')
  }

  return { token, user, isLoggedIn, login, signup, logout }
})
