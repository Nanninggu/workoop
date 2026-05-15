import api from './axios'

export const projectApi = {
  list:   (orgId)               => api.get('/projects', { params: { orgId } }),
  get:    (id)                   => api.get(`/projects/${id}`),
  create: (data)                 => api.post('/projects', data),
  update: (id, data)             => api.put(`/projects/${id}`, data),
  delete: (id)                   => api.delete(`/projects/${id}`),
}

export const taskApi = {
  list:         (projectId)          => api.get('/tasks', { params: { projectId } }),
  mine:         ()                   => api.get('/tasks/mine'),
  create:       (data)               => api.post('/tasks', data),
  update:       (id, data)           => api.put(`/tasks/${id}`, data),
  changeStatus: (id, status)         => api.patch(`/tasks/${id}/status`, { status }),
  delete:       (id)                 => api.delete(`/tasks/${id}`),
}
