import { useState, useCallback } from 'react'
import { Plus, Pencil, Power } from 'lucide-react'
import { tasksApi } from '../api/tasks'
import { smtpApi } from '../api/smtp'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import Modal from '../components/Modal'
import { Field, Input, Select, PrimaryButton, SecondaryButton, ErrorBanner } from '../components/FormControls'
import { useEffect } from 'react'

const TASK_TYPES = ['EMAIL_NOTIFICATION']

export default function Tasks() {
  const [smtpConfigs, setSmtpConfigs] = useState([])
  const [editingTask, setEditingTask] = useState(null)
  const [actionError, setActionError] = useState('')

  const fetchFn = useCallback((page, size) => tasksApi.getAll(page, size), [])
  const { data: tasks, loading, error, refresh, ...pagination } = useServerPagination(fetchFn, 10)

  useEffect(() => {
    smtpApi.getAll(0, 100).then((page) => setSmtpConfigs(page.content)).catch(() => {})
  }, [])

  async function handleDeactivate(id) {
    if (!window.confirm('Deactivate this task?')) return
    try {
      await tasksApi.deactivate(id)
      refresh()
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  const smtpName = (id) => smtpConfigs.find((c) => c.id === id)?.host || `#${id}`

  if (loading && tasks.length === 0) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="Tasks"
        description="Scheduled jobs picked up by the scheduler engine based on their cron expression"
        action={<PrimaryButton onClick={() => setEditingTask({})}><Plus size={16} /> New Task</PrimaryButton>}
      />
      <ErrorBanner message={error || actionError} />
      {tasks.length === 0 && !loading ? (
        <EmptyState title="No tasks yet" description="Create a task to start scheduling." />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-[color:var(--color-border-soft)]">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] text-left text-xs uppercase text-[color:var(--color-ink-muted)]">
                  <th className="px-4 py-3 font-medium">Name</th>
                  <th className="px-4 py-3 font-medium">Type</th>
                  <th className="px-4 py-3 font-medium">Cron</th>
                  <th className="px-4 py-3 font-medium">SMTP</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((task) => (
                  <tr key={task.id} className="border-b border-[color:var(--color-border-soft)] last:border-0 hover:bg-[color:var(--color-surface)] transition-colors">
                    <td className="px-4 py-3 font-medium">{task.name}</td>
                    <td className="px-4 py-3 text-[color:var(--color-ink-muted)]">{task.type}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs">{task.cronExpression}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs text-[color:var(--color-ink-muted)]">{smtpName(task.smtpConfigurationId)}</td>
                    <td className="px-4 py-3"><StatusBadge status={task.isActive ? 'ACTIVE' : 'INACTIVE'} /></td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex justify-end gap-2">
                        <button onClick={() => setEditingTask(task)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] transition-colors" title="Edit"><Pencil size={15} /></button>
                        {task.isActive && <button onClick={() => handleDeactivate(task.id)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-status-failed-soft)] hover:text-[color:var(--color-status-failed)] transition-colors" title="Deactivate"><Power size={15} /></button>}
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
      {editingTask !== null && (
        <TaskFormModal task={editingTask} smtpConfigs={smtpConfigs} onClose={() => setEditingTask(null)} onSaved={() => { setEditingTask(null); refresh() }} />
      )}
    </div>
  )
}

function TaskFormModal({ task, smtpConfigs, onClose, onSaved }) {
  const isEditing = Boolean(task.id)
  const [name, setName] = useState(task.name || '')
  const [type, setType] = useState(task.type || TASK_TYPES[0])
  const [cronExpression, setCronExpression] = useState(task.cronExpression || '')
  const [smtpConfigurationId, setSmtpConfigurationId] = useState(task.smtpConfigurationId || smtpConfigs[0]?.id || '')
  const [isActive, setIsActive] = useState(task.isActive ?? true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSaving(true)
    const payload = { name, type, cronExpression, smtpConfigurationId: Number(smtpConfigurationId), ...(isEditing ? { isActive } : {}) }
    try {
      isEditing ? await tasksApi.update(task.id, payload) : await tasksApi.create(payload)
      onSaved()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isEditing ? 'Edit Task' : 'New Task'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <ErrorBanner message={error} />
        <Field label="Name"><Input value={name} onChange={(e) => setName(e.target.value)} required /></Field>
        <Field label="Type"><Select value={type} onChange={(e) => setType(e.target.value)}>{TASK_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}</Select></Field>
        <Field label="Cron Expression" hint="Spring 6-field format: second minute hour day-of-month month day-of-week. e.g. 0 0 8 * * ?">
          <Input value={cronExpression} onChange={(e) => setCronExpression(e.target.value)} placeholder="0 0 8 * * ?" required />
        </Field>
        <Field label="SMTP Configuration">
          <Select value={smtpConfigurationId} onChange={(e) => setSmtpConfigurationId(e.target.value)} required>
            <option value="" disabled>Select an SMTP configuration</option>
            {smtpConfigs.map((c) => <option key={c.id} value={c.id}>{c.host} ({c.username})</option>)}
          </Select>
        </Field>
        {isEditing && (
          <Field label="Status">
            <Select value={isActive ? 'true' : 'false'} onChange={(e) => setIsActive(e.target.value === 'true')}>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </Select>
          </Field>
        )}
        <div className="mt-6 flex justify-end gap-2">
          <SecondaryButton type="button" onClick={onClose}>Cancel</SecondaryButton>
          <PrimaryButton type="submit" disabled={saving}>{saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create task'}</PrimaryButton>
        </div>
      </form>
    </Modal>
  )
}
