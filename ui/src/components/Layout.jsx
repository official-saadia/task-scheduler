import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, ListChecks, FileText,
  Database, Mail, HardDriveDownload, FileBarChart2, History, AlertTriangle, LogOut,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/tasks', label: 'Tasks', icon: ListChecks },
  { to: '/templates', label: 'Templates', icon: FileText },
  { to: '/template-data', label: 'Template Data', icon: Database },
  { to: '/smtp', label: 'SMTP Configs', icon: Mail },
  { to: '/backup-configurations', label: 'Backup Configs', icon: HardDriveDownload },
  { to: '/report-configurations', label: 'Report Configs', icon: FileBarChart2 },
  { to: '/executions', label: 'Executions', icon: History },
  { to: '/dlq', label: 'Dead Letter Queue', icon: AlertTriangle },
]

function LogoMark({ size = 28 }) {
  const half = size / 2 - 1
  const gap = 2
  const r = 3
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} fill="none" aria-hidden="true">
      <rect x={1}        y={1}        width={half} height={half} rx={r} fill="#4285F4" />
      <rect x={half+gap} y={1}        width={half} height={half} rx={r} fill="#EA4335" />
      <rect x={1}        y={half+gap} width={half} height={half} rx={r} fill="#34A853" />
      <rect x={half+gap} y={half+gap} width={half} height={half} rx={r} fill="#FBBC05" />
    </svg>
  )
}

function LogoWordmark({ size = 'base' }) {
  const text = 'Task Scheduler'
  return (
    <span
      className={`font-semibold leading-none font-[var(--font-display)] text-${size}`}
      style={{ color: 'var(--color-heading)' }}
    >
      {text}
    </span>
  )
}

export default function Layout() {
  const { logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="flex h-screen w-full overflow-hidden bg-white">
      {/* Sidebar */}
      <aside className="flex w-64 flex-col border-r border-[color:var(--color-border-soft)] bg-white shadow-sm">

        {/* Logo */}
        <div className="flex items-center gap-3 px-5 py-5 border-b border-[color:var(--color-border-soft)]">
          <LogoMark size={32} />
          <div className="flex flex-col gap-0.5">
            <LogoWordmark />
            <span className="text-xs text-[color:var(--color-ink-faint)] font-[var(--font-mono)]">console</span>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-0.5">
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                  isActive
                    ? 'text-white bg-[color:var(--color-accent)]'
                    : 'text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)]'
                }`
              }
            >
              <Icon size={17} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Logout */}
        <div className="border-t border-[color:var(--color-border-soft)] p-3">
          <button
            onClick={() => { logout(); navigate('/login') }}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-status-failed-soft)] hover:text-[color:var(--color-status-failed)] transition-colors"
          >
            <LogOut size={17} />
            Log out
          </button>
        </div>
      </aside>

      {/* Main */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top bar */}
        <header className="flex items-center justify-between border-b border-[color:var(--color-border-soft)] bg-white px-8 py-3 shadow-sm">
          <div className="flex items-center gap-2">
            <span className="pulse-dot" />
            <span className="text-xs text-[color:var(--color-ink-muted)] font-[var(--font-mono)]">
              scheduler running
            </span>
          </div>
          <div className="flex items-center gap-3">
            <LogoMark size={24} />
            <LogoWordmark size="sm" />
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto bg-white">
          <div className="mx-auto max-w-6xl px-8 py-8">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
