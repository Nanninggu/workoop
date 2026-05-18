import api from './axios'

export const chatApi = {
  getHistory: (orgId, limit = 50) =>
    api.get('/chat', { params: { orgId, limit } }),

  send: (orgId, content) =>
    api.post('/chat', { content }, { params: { orgId } }),

  askAi: (orgId, orgName, question) =>
    api.post('/chat/ai', { question }, { timeout: 60000 }),

  getAiHistory: (limit = 100) =>
    api.get('/chat/ai/history', { params: { limit } }),
}
