import { apiClient } from './client'

export const backupApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/backup-configurations', { params: { page, size } }).then((res) => res.data),

  getById: (id) => apiClient.get(`/backup-configurations/${id}`).then((res) => res.data),

  create: (data) => apiClient.post('/backup-configurations', data).then((res) => res.data),

  update: (id, data) =>
    apiClient.put(`/backup-configurations/${id}`, data).then((res) => res.data),

  deactivate: (id) =>
    apiClient.patch(`/backup-configurations/${id}/deactivate`).then((res) => res.data),
}
