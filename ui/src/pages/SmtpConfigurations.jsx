import { useState, useCallback } from 'react'
import { Plus, Pencil, Power } from 'lucide-react'
import { smtpApi } from '../api/smtp'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import Modal from '../components/Modal'
import { Field, Input, PrimaryButton, SecondaryButton, ErrorBanner } from '../components/FormControls'

export default function SmtpConfigurations() {
  const [editingConfig, setEditingConfig] = useState(null)
  const [actionError, setActionError] = useState('')

  const fetchFn = useCallback((page, size) => smtpApi.getAll(page, size), [])
  const { data: configs, loading, error, refresh, ...pagination } = useServerPagination(fetchFn, 10)

  async function handleDeactivate(id) {
    if (!window.confirm('Deactivate this SMTP configuration?')) return
    try {
      await smtpApi.deactivate(id)
      refresh()
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  if (loading && configs.length === 0) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="SMTP Configurations"
        description="Mail server credentials used to send email notifications"
        action={<PrimaryButton onClick={() => setEditingConfig({})}><Plus size={16} /> New Configuration</PrimaryButton>}
      />
      <ErrorBanner message={error || actionError} />
      {configs.length === 0 && !loading ? (
        <EmptyState title="No SMTP configurations yet" description="Add one before creating tasks." />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-[color:var(--color-border-soft)]">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] text-left text-xs uppercase text-[color:var(--color-ink-muted)]">
                  <th className="px-4 py-3 font-medium">Host</th>
                  <th className="px-4 py-3 font-medium">Port</th>
                  <th className="px-4 py-3 font-medium">Username</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {configs.map((c) => (
                  <tr key={c.id} className="border-b border-[color:var(--color-border-soft)] last:border-0 hover:bg-[color:var(--color-surface)] transition-colors">
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs">{c.host}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs">{c.port}</td>
                    <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{c.username}</td>
                    <td className="px-4 py-3"><StatusBadge status={c.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button onClick={() => setEditingConfig(c)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] transition-colors" title="Edit"><Pencil size={15} /></button>
                        {c.isActive && <button onClick={() => handleDeactivate(c.id)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-status-failed-soft)] hover:text-[color:var(--color-status-failed)] transition-colors" title="Deactivate"><Power size={15} /></button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            currentPage={pagination.currentPageDisplay}
            totalPages={pagination.totalPagesDisplay}
            totalItems={pagination.totalItems}
            pageSize={10}
            goToPage={pagination.goToPage}
            goToPrev={pagination.goToPrev}
            goToNext={pagination.goToNext}
            hasPrev={pagination.hasPrev}
            hasNext={pagination.hasNext}
          />
        </>
      )}
      {editingConfig !== null && (
        <SmtpFormModal config={editingConfig} onClose={() => setEditingConfig(null)} onSaved={() => { setEditingConfig(null); refresh() }} />
      )}
    </div>
  )
}

function SmtpFormModal({ config, onClose, onSaved }) {
  const isEditing = Boolean(config.id)
  const [host, setHost] = useState(config.host || 'smtp.gmail.com')
  const [port, setPort] = useState(config.port || 587)
  const [username, setUsername] = useState(config.username || '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      const payload = { host, port: Number(port), username, password }
      isEditing ? await smtpApi.update(config.id, payload) : await smtpApi.create(payload)
      onSaved()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isEditing ? 'Edit SMTP Configuration' : 'New SMTP Configuration'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ErrorBanner message={error} />
        <Field label="Host"><Input value={host} onChange={(e) => setHost(e.target.value)} required /></Field>
        <Field label="Port"><Input type="number" value={port} onChange={(e) => setPort(e.target.value)} required /></Field>
        <Field label="Username (also used as From address)"><Input type="email" value={username} onChange={(e) => setUsername(e.target.value)} required /></Field>
        <Field label="Password" hint="For Gmail, use an App Password."><Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></Field>
        <div className="mt-6 flex justify-end gap-2">
          <SecondaryButton type="button" onClick={onClose}>Cancel</SecondaryButton>
          <PrimaryButton type="submit" disabled={saving}>{saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create configuration'}</PrimaryButton>
        </div>
      </form>
    </Modal>
  )
}
