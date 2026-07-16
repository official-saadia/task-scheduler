import { apiClient } from './client'

export const executionsApi = {
  getAll: (page = 0, size = 15) =>
    apiClient.get('/task-executions', { params: { page, size } }).then((res) => res.data),

  getByTaskId: (taskId, page = 0, size = 15) =>
    apiClient.get(`/task-executions/task/${taskId}`, { params: { page, size } }).then((res) => res.data),

  getByStatus: (status, page = 0, size = 15) =>
    apiClient.get(`/task-executions/status/${status}`, { params: { page, size } }).then((res) => res.data),
}

export const dlqApi = {
  getAll: (page = 0, size = 10) =>
    apiClient.get('/task-dlq', { params: { page, size } }).then((res) => res.data),

  getByStatus: (status, page = 0, size = 10) =>
    apiClient.get(`/task-dlq/status/${status}`, { params: { page, size } }).then((res) => res.data),

  updateStatus: (id, status) =>
    apiClient.patch(`/task-dlq/${id}/status`, null, { params: { status } }).then((res) => res.data),

  // params is either { dateRange } for a preset window,
  // or { from, to } (ISO yyyy-MM-dd) for a custom one.
  export: (params) =>
    apiClient
      .get('/task-dlq/export', { params, responseType: 'blob' })
      .then((res) => res.data),
}
