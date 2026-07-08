import { apiClient } from './client'

export const analyticsApi = {
  get: () => apiClient.get('/analytics').then((res) => res.data),
}
