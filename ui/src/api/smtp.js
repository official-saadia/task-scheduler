import { apiClient } from './client'

export const smtpApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/smtp-configurations', { params: { page, size } }).then((res) => res.data),

  getById: (id) => apiClient.get(`/smtp-configurations/${id}`).then((res) => res.data),

  create: (data) => apiClient.post('/smtp-configurations', data).then((res) => res.data),

  update: (id, data) =>
    apiClient.put(`/smtp-configurations/${id}`, data).then((res) => res.data),

  deactivate: (id) =>
    apiClient.patch(`/smtp-configurations/${id}/deactivate`).then((res) => res.data),
}
