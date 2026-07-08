import { useEffect, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { tasksApi } from '../api/tasks'
import { templatesApi } from '../api/templates'
import { taskTemplateDataApi } from '../api/taskTemplateData'
import { getErrorMessage } from '../api/client'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import {
  Field, Select, Textarea, PrimaryButton, DangerButton, ErrorBanner,
} from '../components/FormControls'

const DATA_PLACEHOLDER = JSON.stringify(
  [
    { recipient: 'john@example.com', recipientName: 'John Doe', username: 'johndoe' },
    { recipient: 'jane@example.com', recipientName: 'Jane Smith', username: 'janesmith' },
  ],
  null, 2
)

export default function TaskTemplateData() {
  const [tasks, setTasks] = useState([])
  const [templates, setTemplates] = useState([])
  const [selectedTaskId, setSelectedTaskId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    // Fetch with large page size to get all tasks and templates for the dropdowns
    Promise.all([
      tasksApi.getAll(0, 100),
      templatesApi.getAll(0, 100),
    ])
      .then(([taskPage, templatePage]) => {
        // Both APIs now return Spring Page objects — extract .content
        const taskList = taskPage.content ?? []
        const templateList = templatePage.content ?? []
        setTasks(taskList)
        setTemplates(templateList)
        if (taskList.length > 0) setSelectedTaskId(String(taskList[0].id))
      })
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  return (
    <div>
      <PageHeader
        title="Template Data"
        description="The runtime JSON payload each task uses to resolve template placeholders — a list of recipients with personalized values"
      />

      <ErrorBanner message={error} />

      {tasks.length === 0 ? (
        <EmptyState
          title="No tasks yet"
          description="Create a task first, then attach template data to it here."
        />
      ) : (
        <>
          <div className="mb-4 max-w-sm">
            <Field label="Select Task">
              <Select value={selectedTaskId} onChange={(e) => setSelectedTaskId(e.target.value)}>
                {tasks.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} (#{t.id})
                  </option>
                ))}
              </Select>
            </Field>
          </div>

          {selectedTaskId && (
            <TemplateDataEditor
              key={selectedTaskId}
              taskId={Number(selectedTaskId)}
              templates={templates}
            />
          )}
        </>
      )}
    </div>
  )
}

function TemplateDataEditor({ taskId, templates }) {
  const [record, setRecord] = useState(null)
  const [templateId, setTemplateId] = useState(templates[0]?.id || '')
  const [data, setData] = useState(DATA_PLACEHOLDER)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    setSuccess('')
    taskTemplateDataApi
      .getByTaskId(taskId)
      .then((result) => {
        setRecord(result)
        if (result) {
          setTemplateId(result.template?.id || templates[0]?.id || '')
          setData(formatJson(result.data))
        } else {
          setTemplateId(templates[0]?.id || '')
          setData(DATA_PLACEHOLDER)
        }
      })
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [taskId])

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      JSON.parse(data)
    } catch {
      setError('Data must be valid JSON.')
      return
    }
    setSaving(true)
    try {
      if (record) {
        const updated = await taskTemplateDataApi.update(record.id, { data })
        setRecord(updated)
      } else {
        const created = await taskTemplateDataApi.create({
          taskId,
          templateId: Number(templateId),
          data,
        })
        setRecord(created)
      }
      setSuccess('Saved successfully.')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!record) return
    if (!window.confirm('Delete template data for this task?')) return
    try {
      await taskTemplateDataApi.remove(record.id)
      setRecord(null)
      setData(DATA_PLACEHOLDER)
      setTemplateId(templates[0]?.id || '')
      setSuccess('Deleted.')
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  if (loading) return <LoadingState />

  return (
    <div className="rounded-xl border border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] p-5">
      <ErrorBanner message={error} />
      {success && (
        <p className="mb-4 text-sm" style={{ color: 'var(--color-status-success)' }}>
          {success}
        </p>
      )}

      <form onSubmit={handleSubmit}>
        <Field label="Template" hint={record ? 'Template cannot be changed after creation.' : ''}>
          <Select
            value={templateId}
            onChange={(e) => setTemplateId(e.target.value)}
            disabled={Boolean(record)}
            required
          >
            <option value="" disabled>Select a template</option>
            {templates.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </Select>
        </Field>

        <Field
          label="Recipients / Data (JSON array)"
          hint='Each entry must include a "recipient" field (email address). All other keys are template placeholders.'
        >
          <Textarea value={data} onChange={(e) => setData(e.target.value)} rows={14} required />
        </Field>

        <div className="flex justify-between">
          {record ? (
            <DangerButton type="button" onClick={handleDelete}>
              <Trash2 size={15} /> Delete
            </DangerButton>
          ) : (
            <span />
          )}
          <PrimaryButton type="submit" disabled={saving}>
            {saving ? 'Saving...' : record ? 'Save changes' : 'Create'}
          </PrimaryButton>
        </div>
      </form>
    </div>
  )
}

function formatJson(raw) {
  try { return JSON.stringify(JSON.parse(raw), null, 2) } catch { return raw }
}
