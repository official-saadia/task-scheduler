import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getErrorMessage } from '../api/client'
import { Field, Input, PrimaryButton, ErrorBanner } from '../components/FormControls'

function LogoMark() {
  return (
    <svg width="40" height="40" viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <rect x="1"  y="1"  width="18" height="18" rx="4" fill="#4285F4" />
      <rect x="21" y="1"  width="18" height="18" rx="4" fill="#EA4335" />
      <rect x="1"  y="21" width="18" height="18" rx="4" fill="#34A853" />
      <rect x="21" y="21" width="18" height="18" rx="4" fill="#FBBC05" />
    </svg>
  )
}

function LogoWordmark() {
  const text = 'Task Scheduler'
  return (
    <span
      className="text-2xl font-semibold leading-none font-[var(--font-display)]"
      style={{ color: 'var(--color-heading)' }}
    >
      {text}
    </span>
  )
}

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate(location.state?.from || '/', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err) || 'Invalid username or password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex h-screen items-center justify-center bg-white px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <LogoMark />
          <div className="mt-3"><LogoWordmark /></div>
          <p className="mt-2 text-sm text-[color:var(--color-ink-muted)]">
            Sign in to the operations console
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-xl border border-[color:var(--color-border-soft)] bg-white p-6 shadow-sm"
        >
          <ErrorBanner message={error} />

          <Field label="Username">
            <Input type="text" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          </Field>

          <Field label="Password">
            <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </Field>

          <PrimaryButton type="submit" disabled={loading} className="w-full mt-2">
            {loading ? 'Signing in...' : 'Sign in'}
          </PrimaryButton>
        </form>

        <p className="mt-4 text-center text-xs text-[color:var(--color-ink-faint)] font-[var(--font-mono)]">
          default: admin / admin123
        </p>
      </div>
    </div>
  )
}
