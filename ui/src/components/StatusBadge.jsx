const STATUS_STYLES = {
  PENDING:        { color: '#4285F4', bg: '#e8f0fe' },
  IN_PROGRESS:    { color: '#b06000', bg: '#fef9e0' },
  COMPLETED:      { color: '#1e7e34', bg: '#e6f4ea' },
  PARTIAL_SUCCESS:{ color: '#1e7e34', bg: '#e6f4ea' },
  FAILED:         { color: '#c5221f', bg: '#fce8e6' },
  SUCCESS:        { color: '#1e7e34', bg: '#e6f4ea' },
  NEW:            { color: '#c5221f', bg: '#fce8e6' },
  ANALYSED:       { color: '#4285F4', bg: '#e8f0fe' },
  FIXED:          { color: '#1e7e34', bg: '#e6f4ea' },
  ACTIVE:         { color: '#1e7e34', bg: '#e6f4ea' },
  INACTIVE:       { color: '#5f6368', bg: '#f1f3f4' },
}

export default function StatusBadge({ status }) {
  const style = STATUS_STYLES[status] || { color: '#5f6368', bg: '#f1f3f4' }

  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium font-[var(--font-mono)]"
      style={{ color: style.color, backgroundColor: style.bg }}
    >
      <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: style.color }} />
      {status}
    </span>
  )
}
