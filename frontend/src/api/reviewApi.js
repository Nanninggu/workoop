import api from './axios'

export const reviewApi = {
  list:   ()     => api.get('/reviews'),
  create: (data) => api.post('/reviews', data),
  delete: (id)   => api.delete(`/reviews/${id}`),
}
