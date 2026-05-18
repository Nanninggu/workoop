import api from './axios'

export const scrumApi = {
  save:  (data)               => api.post('/scrums', data),
  me:    (date)               => api.get('/scrums/me', { params: { date } }),
  range: (startDate, endDate) => api.get('/scrums/me/range', { params: { startDate, endDate } }),
  team:  (orgId, date)        => api.get('/scrums/team', { params: { orgId, date } }),
}

export const syncApi = {
  slack: (orgId) => api.post('/sync/slack', { orgId }),
}
