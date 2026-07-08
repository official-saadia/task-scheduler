import { apiClient } from './client'

export const tasksApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/tasks', { params: { page, size } }).then((res) => res.data),

  getById: (id) => apiClient.get(`/tasks/${id}`).then((res) => res.data),

  create: (data) => apiClient.post('/tasks', data).then((res) => res.data),

  update: (id, data) => apiClient.put(`/tasks/${id}`, data).then((res) => res.data),

  deactivate: (id) => apiClient.patch(`/tasks/${id}/deactivate`).then((res) => res.data),
}
