import api from './axios'

export const kpiApi = {
  getAll: (params) => api.get('/kpis', { params }),
  getById: (id) => api.get(`/kpis/${id}`),
  create: (data) => api.post('/kpis', data),
  update: (id, data) => api.put(`/kpis/${id}`, data),
  updateStatus: (id, status) => api.patch(`/kpis/${id}/status`, { status }),
  delete: (id) => api.delete(`/kpis/${id}`),
  copy: (id) => api.post(`/kpis/${id}/copy`)
}

export const recordApi = {
  getByDate: (date) => api.get('/records', { params: { date } }),
  getByDateRange: (startDate, endDate) => api.get('/records/range', { params: { startDate, endDate } }),
  getByKpiId: (kpiId, params) => api.get(`/records/kpi/${kpiId}`, { params }),
  upsert: (data) => api.post('/records', data),
  delete: (id) => api.delete(`/records/${id}`)
}

export const dashboardApi = {
  get: () => api.get('/dashboard')
}

export const exportApi = {
  csv: (startDate, endDate) => {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate)   params.append('endDate', endDate)
    const url = `/api/export/csv?${params.toString()}`
    const a = document.createElement('a')
    a.href = url
    a.download = `workoop_export_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  },
  json: () => {
    const a = document.createElement('a')
    a.href = '/api/export/json'
    a.download = `workoop_backup_${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  },
  importJson: (data) => api.post('/export/json/import', data)
}
