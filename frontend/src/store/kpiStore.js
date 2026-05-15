import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kpiApi, recordApi, dashboardApi } from '@/api/kpiApi'
import { categoryApi } from '@/api/categoryApi'
import { ElMessage } from 'element-plus'

export const useKpiStore = defineStore('kpi', () => {
  const kpis = ref([])
  const categories = ref([])
  const dashboard = ref(null)
  const loading = ref(false)

  const activeKpis = computed(() => kpis.value.filter(k => k.status === 'ACTIVE'))

  async function fetchKpis(params) {
    loading.value = true
    try {
      const res = await kpiApi.getAll(params)
      kpis.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    const res = await categoryApi.getAll()
    categories.value = res.data
  }

  async function fetchDashboard() {
    loading.value = true
    try {
      const res = await dashboardApi.get()
      dashboard.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function createKpi(data) {
    const res = await kpiApi.create(data)
    ElMessage.success('KPI가 생성되었습니다.')
    await fetchKpis()
    return res.data
  }

  async function updateKpi(id, data) {
    const res = await kpiApi.update(id, data)
    ElMessage.success('KPI가 수정되었습니다.')
    await fetchKpis()
    return res.data
  }

  async function deleteKpi(id) {
    await kpiApi.delete(id)
    ElMessage.success('KPI가 삭제되었습니다.')
    await fetchKpis()
  }

  async function saveRecord(data) {
    const res = await recordApi.upsert(data)
    ElMessage.success('실적이 저장되었습니다.')
    return res.data
  }

  return {
    kpis, categories, dashboard, loading, activeKpis,
    fetchKpis, fetchCategories, fetchDashboard,
    createKpi, updateKpi, deleteKpi, saveRecord
  }
})
