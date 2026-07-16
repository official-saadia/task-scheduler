import { apiClient } from './client'

export const reportsApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/report-configurations', { params: { page, size } }).then((res) => res.data),

  getById: (id) => apiClient.get(`/report-configurations/${id}`).then((res) => res.data),

  create: (data) => apiClient.post('/report-configurations', data).then((res) => res.data),

  update: (id, data) =>
    apiClient.put(`/report-configurations/${id}`, data).then((res) => res.data),

  deactivate: (id) =>
    apiClient.patch(`/report-configurations/${id}/deactivate`).then((res) => res.data),
}
