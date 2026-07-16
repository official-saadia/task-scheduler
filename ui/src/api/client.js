import axios from 'axios'

// Base URL for the Spring Boot backend.
// Set VITE_API_BASE_URL in a .env file to override (see .env.example).
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Attach the JWT token to every outgoing request, if present.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// If the token is invalid or expired, the API returns 401.
// Clear the stored token and send the user back to the login page.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

/**
 * Extracts a readable error message from an API error response.
 * The backend's GlobalExceptionHandler returns { message, error, status, ... }.
 */
export function getErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    'Something went wrong. Please try again.'
  )
}

/**
 * Blob-aware variant of getErrorMessage.
 *
 * Requests made with `responseType: 'blob'` (e.g. the DLQ XLSX export) receive
 * their *error* bodies as Blobs too, so `error.response.data.message` is
 * undefined and getErrorMessage falls back to a useless "Request failed with
 * status code 400". This reads the Blob back as text and recovers the
 * GlobalExceptionHandler payload.
 */
export async function getErrorMessageAsync(error) {
  const data = error?.response?.data
  if (data instanceof Blob) {
    try {
      const parsed = JSON.parse(await data.text())
      return parsed.message || parsed.error || getErrorMessage(error)
    } catch {
      // Not JSON — fall through to the sync reader.
    }
  }
  return getErrorMessage(error)
}
