import { apiClient } from './client'

export const taskTemplateDataApi = {
  /**
   * Returns the template data for a task, or null if none exists yet (404).
   */
  getByTaskId: (taskId) =>
    apiClient
      .get(`/task-template-data/task/${taskId}`)
      .then((res) => res.data)
      .catch((err) => {
        if (err.response?.status === 404) return null
        throw err
      }),

  create: (data) => apiClient.post('/task-template-data', data).then((res) => res.data),

  update: (id, data) => apiClient.put(`/task-template-data/${id}`, data).then((res) => res.data),

  remove: (id) => apiClient.delete(`/task-template-data/${id}`).then((res) => res.data),
}
