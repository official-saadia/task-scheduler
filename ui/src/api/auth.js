import { apiClient } from './client'

/**
 * Logs in with username and password.
 * Returns { token, tokenType } on success.
 */
export function login(username, password) {
  return apiClient.post('/auth/login', { username, password }).then((res) => res.data)
}
