import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('coop-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('coop-token')
      localStorage.removeItem('coop-user')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    const message = error.response?.data?.message || '서버와의 통신 중 오류가 발생했습니다.'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default api
