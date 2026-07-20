import { createContext, useContext, useState, useCallback, useMemo } from 'react'
import { login as loginRequest } from '../api/auth'

const AuthContext = createContext(null)

/**
 * Reads the `sub` (username) claim out of a JWT for display purposes only.
 * Never trust this for authorization — the server verifies the signature.
 */
function usernameFromToken(token) {
  if (!token) return null
  try {
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(json).sub ?? null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))

  const login = useCallback(async (username, password) => {
    const data = await loginRequest(username, password)
    localStorage.setItem('token', data.token)
    setToken(data.token)
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    setToken(null)
  }, [])

  const username = useMemo(() => usernameFromToken(token), [token])

  const value = {
    token,
    username,
    isAuthenticated: Boolean(token),
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
