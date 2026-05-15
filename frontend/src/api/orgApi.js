import api from './axios'

export const orgApi = {
  list:           ()                    => api.get('/organizations'),
  create:         (name)                => api.post('/organizations', { name }),
  members:        (orgId)               => api.get(`/organizations/${orgId}/members`),
  generateInvite: (orgId)               => api.post(`/organizations/${orgId}/invite`),
  joinByCode:     (code)                => api.post('/organizations/join', { code }),
  removeMember:   (orgId, userId)       => api.delete(`/organizations/${orgId}/members/${userId}`),
  changeRole:     (orgId, userId, role) => api.patch(`/organizations/${orgId}/members/${userId}/role`, { role }),
}
