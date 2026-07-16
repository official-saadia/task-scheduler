import { useState, useCallback } from 'react'
import { Plus, Pencil, Power } from 'lucide-react'
import { backupApi } from '../api/backup'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import Modal from '../components/Modal'
import { Field, Input, Textarea, PrimaryButton, SecondaryButton, ErrorBanner } from '../components/FormControls'

export default function BackupConfigurations() {
  const [editingConfig, setEditingConfig] = useState(null)
  const [actionError, setActionError] = useState('')

  const fetchFn = useCallback((page, size) => backupApi.getAll(page, size), [])
  const { data: configs, loading, error, refresh, ...pagination } = useServerPagination(fetchFn, 10)

  async function handleDeactivate(id) {
    if (!window.confirm('Deactivate this backup configuration?')) return
    try {
      await backupApi.deactivate(id)
      refresh()
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  if (loading && configs.length === 0) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="Backup Configurations"
        description="Shell commands Task Scheduler runs to trigger external database backups"
        action={<PrimaryButton onClick={() => setEditingConfig({})}><Plus size={16} /> New Configuration</PrimaryButton>}
      />
      <ErrorBanner message={error || actionError} />
      {configs.length === 0 && !loading ? (
        <EmptyState title="No backup configurations yet" description="Add one before creating database backup tasks." />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-[color:var(--color-border-soft)]">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] text-left text-xs uppercase text-[color:var(--color-ink-muted)]">
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Command</th>
                  <th className="px-4 py-3 font-medium">Timeout</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {configs.map((c) => (
                  <tr key={c.id} className="border-b border-[color:var(--color-border-soft)] last:border-0 odd:bg-white even:bg-[color:var(--color-row-alt)] hover:bg-[color:var(--color-status-scheduled-soft)] transition-colors">
                    <td className="px-4 py-3">{c.name}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs max-w-xs truncate" title={c.command}>{c.command}</td>
                    <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{c.timeoutSeconds}s</td>
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
        <BackupFormModal config={editingConfig} onClose={() => setEditingConfig(null)} onSaved={() => { setEditingConfig(null); refresh() }} />
      )}
    </div>
  )
}

function BackupFormModal({ config, onClose, onSaved }) {
  const isEditing = Boolean(config.id)
  const [name, setName] = useState(config.name || '')
  const [command, setCommand] = useState(config.command || '')
  const [workingDirectory, setWorkingDirectory] = useState(config.workingDirectory || '')
  const [timeoutSeconds, setTimeoutSeconds] = useState(config.timeoutSeconds || 300)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      const payload = {
        name,
        command,
        workingDirectory: workingDirectory || null,
        timeoutSeconds: Number(timeoutSeconds),
      }
      isEditing ? await backupApi.update(config.id, payload) : await backupApi.create(payload)
      onSaved()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isEditing ? 'Edit Backup Configuration' : 'New Backup Configuration'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ErrorBanner message={error} />
        <Field label="Name"><Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Nightly Postgres backup" required /></Field>
        <Field
          label="Command"
          hint="Task Scheduler only runs this command and checks its exit code — it does not know how the backup itself works. Exit code 0 = success."
        >
          <Textarea rows={3} value={command} onChange={(e) => setCommand(e.target.value)} placeholder="/opt/scripts/backup.sh" required />
        </Field>
        <Field label="Working directory" hint="Optional. Defaults to the server's working directory.">
          <Input value={workingDirectory} onChange={(e) => setWorkingDirectory(e.target.value)} placeholder="/opt/scripts" />
        </Field>
        <Field label="Timeout (seconds)" hint="The command is force-killed and treated as failed if it runs longer than this.">
          <Input type="number" min="1" max="86400" value={timeoutSeconds} onChange={(e) => setTimeoutSeconds(e.target.value)} required />
        </Field>
        <div className="mt-6 flex justify-end gap-2">
          <SecondaryButton type="button" onClick={onClose}>Cancel</SecondaryButton>
          <PrimaryButton type="submit" disabled={saving}>{saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create configuration'}</PrimaryButton>
        </div>
      </form>
    </Modal>
  )
}
