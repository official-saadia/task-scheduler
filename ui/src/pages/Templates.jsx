import { useState, useCallback } from 'react'
import { Plus, Pencil, Power } from 'lucide-react'
import { templatesApi } from '../api/templates'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import Modal from '../components/Modal'
import { Field, Input, Textarea, PrimaryButton, SecondaryButton, ErrorBanner } from '../components/FormControls'

const TEMPLATE_PLACEHOLDER = JSON.stringify(
  { subject: 'Welcome {{recipientName}}!', body: 'Dear {{recipientName}},\n\nYour username is {{username}}.', to: '{{recipient}}' },
  null, 2
)

function formatJson(raw) {
  try { return JSON.stringify(JSON.parse(raw), null, 2) } catch { return raw }
}

export default function Templates() {
  const [editingTemplate, setEditingTemplate] = useState(null)
  const [actionError, setActionError] = useState('')

  const fetchFn = useCallback((page, size) => templatesApi.getAll(page, size), [])
  const { data: templates, loading, error, refresh, ...pagination } = useServerPagination(fetchFn, 5)

  async function handleDeactivate(id) {
    if (!window.confirm('Deactivate this template?')) return
    try {
      await templatesApi.deactivate(id)
      refresh()
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  if (loading && templates.length === 0) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="Templates"
        description="JSON-structured email templates with subject, body, and recipient placeholders"
        action={<PrimaryButton onClick={() => setEditingTemplate({})}><Plus size={16} /> New Template</PrimaryButton>}
      />
      <ErrorBanner message={error || actionError} />
      {templates.length === 0 && !loading ? (
        <EmptyState title="No templates yet" description="Create a template to define the structure of your emails." />
      ) : (
        <>
          <div className="space-y-3">
            {templates.map((t) => (
              <div key={t.id} className="rounded-xl border border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-medium">{t.name}</h3>
                      <StatusBadge status={t.isActive ? 'ACTIVE' : 'INACTIVE'} />
                      <span className="font-[var(--font-mono)] text-xs text-[color:var(--color-ink-faint)]">#{t.id}</span>
                    </div>
                    <pre className="mt-2 max-w-2xl overflow-x-auto rounded-lg bg-[color:var(--color-surface-raised)] border border-[color:var(--color-border-soft)] p-3 text-xs font-[var(--font-mono)] text-[color:var(--color-ink-muted)]">{formatJson(t.template)}</pre>
                  </div>
                  <div className="flex gap-2 shrink-0">
                    <button onClick={() => setEditingTemplate(t)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-surface-raised)] hover:text-[color:var(--color-ink)] transition-colors" title="Edit"><Pencil size={15} /></button>
                    {t.isActive && <button onClick={() => handleDeactivate(t.id)} className="rounded-md p-1.5 text-[color:var(--color-ink-muted)] hover:bg-[color:var(--color-status-failed-soft)] hover:text-[color:var(--color-status-failed)] transition-colors" title="Deactivate"><Power size={15} /></button>}
                  </div>
                </div>
              </div>
            ))}
          </div>
          <Pagination
            currentPage={pagination.currentPageDisplay}
            totalPages={pagination.totalPagesDisplay}
            totalItems={pagination.totalItems}
            pageSize={5}
            goToPage={pagination.goToPage}
            goToPrev={pagination.goToPrev}
            goToNext={pagination.goToNext}
            hasPrev={pagination.hasPrev}
            hasNext={pagination.hasNext}
          />
        </>
      )}
      {editingTemplate !== null && (
        <TemplateFormModal template={editingTemplate} onClose={() => setEditingTemplate(null)} onSaved={() => { setEditingTemplate(null); refresh() }} />
      )}
    </div>
  )
}

function TemplateFormModal({ template, onClose, onSaved }) {
  const isEditing = Boolean(template.id)
  const [name, setName] = useState(template.name || '')
  const [body, setBody] = useState(template.template ? formatJson(template.template) : TEMPLATE_PLACEHOLDER)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try { JSON.parse(body) } catch { setError('Template must be valid JSON.'); return }
    setSaving(true)
    try {
      const payload = { name, template: body }
      isEditing ? await templatesApi.update(template.id, payload) : await templatesApi.create(payload)
      onSaved()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal title={isEditing ? 'Edit Template' : 'New Template'} onClose={onClose} wide>
      <form onSubmit={handleSubmit}>
        <ErrorBanner message={error} />
        <Field label="Name"><Input value={name} onChange={(e) => setName(e.target.value)} required /></Field>
        <Field label="Template JSON" hint='Must include "subject", "body", and "to" fields. Use {{placeholder}} syntax.'>
          <Textarea value={body} onChange={(e) => setBody(e.target.value)} rows={12} required />
        </Field>
        <div className="mt-6 flex justify-end gap-2">
          <SecondaryButton type="button" onClick={onClose}>Cancel</SecondaryButton>
          <PrimaryButton type="submit" disabled={saving}>{saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create template'}</PrimaryButton>
        </div>
      </form>
    </Modal>
  )
}
