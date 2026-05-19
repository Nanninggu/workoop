import api from './axios'

export const starApi = {
  list:   ()         => api.get('/star-notes'),
  create: (data)     => api.post('/star-notes', data),
  update: (id, data) => api.put(`/star-notes/${id}`, data),
  delete: (id)       => api.delete(`/star-notes/${id}`),
}
