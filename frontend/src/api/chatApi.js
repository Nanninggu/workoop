import api from './axios'

export const chatApi = {
  getHistory: (orgId, limit = 50) =>
    api.get('/chat', { params: { orgId, limit } }),

  send: (orgId, content) =>
    api.post('/chat', { content }, { params: { orgId } }),

  askAi: (orgId, orgName, question) =>
    api.post('/ai/chat', { question }, { params: { orgId, orgName } }),
}
