export function StatCard({ label, value, accent = 'scheduled', sub }) {
  const colorVar = `var(--color-status-${accent})`

  return (
    <div
      className="rounded-xl border border-l-4 bg-[color:var(--color-surface)] p-5 shadow-sm"
      style={{ borderLeftColor: colorVar, borderColor: 'var(--color-border-soft)', borderLeftWidth: '4px' }}
    >
      <p className="text-xs font-medium uppercase tracking-wide text-[color:var(--color-ink-muted)]">
        {label}
      </p>
      <p className="mt-2 text-3xl font-[var(--font-display)] font-semibold text-[color:var(--color-ink)]">
        {value}
      </p>
      {sub && <p className="mt-1 text-xs text-[color:var(--color-ink-muted)]">{sub}</p>}
    </div>
  )
}

export function PageHeader({ title, description, action }) {
  return (
    <div className="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 className="text-2xl font-semibold">{title}</h1>
        {description && (
          <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">{description}</p>
        )}
      </div>
      {action}
    </div>
  )
}

export function EmptyState({ title, description }) {
  return (
    <div className="rounded-xl border border-dashed border-[color:var(--color-border-soft)] py-16 text-center bg-[color:var(--color-surface)]">
      <p className="text-sm font-medium text-[color:var(--color-ink)]">{title}</p>
      {description && (
        <p className="mt-1 text-sm text-[color:var(--color-ink-muted)]">{description}</p>
      )}
    </div>
  )
}

export function LoadingState() {
  return (
    <div className="flex items-center justify-center py-16 text-sm text-[color:var(--color-ink-muted)]">
      Loading...
    </div>
  )
}
