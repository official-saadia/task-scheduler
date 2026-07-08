export function Field({ label, hint, children }) {
  return (
    <label className="block mb-4">
      <span className="block text-sm font-medium text-[color:var(--color-ink)] mb-1.5">{label}</span>
      {children}
      {hint && <span className="block text-xs text-[color:var(--color-ink-muted)] mt-1">{hint}</span>}
    </label>
  )
}

const baseInputClasses =
  'w-full rounded-lg border border-[color:var(--color-border-soft)] bg-white px-3 py-2 text-sm text-[color:var(--color-ink)] placeholder:text-[color:var(--color-ink-faint)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-accent)] focus:border-transparent transition-shadow'

export function Input(props) {
  return <input className={baseInputClasses} {...props} />
}

export function Textarea(props) {
  return <textarea className={`${baseInputClasses} font-[var(--font-mono)] resize-y`} {...props} />
}

export function Select({ children, ...props }) {
  return (
    <select className={baseInputClasses} {...props}>
      {children}
    </select>
  )
}

export function PrimaryButton({ children, ...props }) {
  return (
    <button
      className="inline-flex items-center justify-center gap-2 rounded-lg bg-[color:var(--color-accent)] px-4 py-2 text-sm font-medium text-white hover:opacity-90 active:opacity-80 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
      {...props}
    >
      {children}
    </button>
  )
}

export function SecondaryButton({ children, ...props }) {
  return (
    <button
      className="inline-flex items-center justify-center gap-2 rounded-lg border border-[color:var(--color-border-soft)] bg-white px-4 py-2 text-sm font-medium text-[color:var(--color-ink)] hover:bg-[color:var(--color-surface-raised)] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      {...props}
    >
      {children}
    </button>
  )
}

export function DangerButton({ children, ...props }) {
  return (
    <button
      className="inline-flex items-center justify-center gap-2 rounded-lg border border-[color:var(--color-status-failed)] bg-white px-4 py-2 text-sm font-medium text-[color:var(--color-status-failed)] hover:bg-[color:var(--color-status-failed-soft)] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      {...props}
    >
      {children}
    </button>
  )
}

export function ErrorBanner({ message }) {
  if (!message) return null
  return (
    <div
      className="mb-4 rounded-lg border px-3 py-2 text-sm"
      style={{
        borderColor: 'var(--color-status-failed)',
        backgroundColor: 'var(--color-status-failed-soft)',
        color: '#c5221f',
      }}
    >
      {message}
    </div>
  )
}
