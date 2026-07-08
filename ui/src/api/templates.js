import { apiClient } from './client'

export const templatesApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/templates', { params: { page, size } }).then((res) => res.data),

  getById: (id) => apiClient.get(`/templates/${id}`).then((res) => res.data),

  create: (data) => apiClient.post('/templates', data).then((res) => res.data),

  update: (id, data) => apiClient.put(`/templates/${id}`, data).then((res) => res.data),

  deactivate: (id) => apiClient.patch(`/templates/${id}/deactivate`).then((res) => res.data),
}
