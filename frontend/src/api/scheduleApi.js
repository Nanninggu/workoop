import api from './axios'

export const scheduleApi = {
  list: (from, to) => api.get('/schedules', { params: { from, to } }),
  create: (title, eventDate, eventTime) =>
    api.post('/schedules', { title, eventDate, eventTime }),
  remove: (id) => api.delete(`/schedules/${id}`),
}
