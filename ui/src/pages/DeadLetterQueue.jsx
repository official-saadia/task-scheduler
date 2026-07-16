import { useState, useCallback, useMemo } from 'react'
import { Download } from 'lucide-react'
import { dlqApi } from '../api/executions'
import { getErrorMessage, getErrorMessageAsync } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import { Select, Input, Field, ErrorBanner, PrimaryButton } from '../components/FormControls'

const STATUS_OPTIONS = ['ALL', 'NEW', 'IN_PROGRESS', 'ANALYSED', 'FIXED']
const NEXT_STATUS = { NEW: 'IN_PROGRESS', IN_PROGRESS: 'ANALYSED', ANALYSED: 'FIXED' }

const CUSTOM = 'CUSTOM'
const EXPORT_RANGES = [
  { value: 'TODAY', label: 'Today' },
  { value: 'YESTERDAY', label: 'Yesterday' },
  { value: 'PAST_7_DAYS', label: 'Past 7 days' },
  { value: 'PAST_30_DAYS', label: 'Past 30 days' },
  { value: CUSTOM, label: 'Custom range...' },
]

// en-CA formats as yyyy-MM-dd in local time, which is what <input type="date">
// and the backend's ISO.DATE binding both expect. toISOString() would be UTC
// and can land on the wrong day.
const todayIso = () => new Date().toLocaleDateString('en-CA')

export default function DeadLetterQueue() {
  const [status, setStatus] = useState('ALL')
  const [exportRange, setExportRange] = useState('TODAY')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [exporting, setExporting] = useState(false)
  const [actionError, setActionError] = useState('')

  const isCustom = exportRange === CUSTOM

  // Mirrors the backend's validation so we fail fast without a round trip.
  const customError = useMemo(() => {
    if (!isCustom || !customFrom || !customTo) return ''
    if (customTo < customFrom) return 'The "to" date must not be before the "from" date.'
    return ''
  }, [isCustom, customFrom, customTo])

  const canExport = !exporting && (!isCustom || (customFrom && customTo && !customError))

  const fetchFn = useCallback(
    (page, size) =>
      status === 'ALL'
        ? dlqApi.getAll(page, size)
        : dlqApi.getByStatus(status, page, size),
    [status]
  )

  const { data: entries, loading, error, refresh, ...pagination } = useServerPagination(fetchFn, 10, [status])

  async function handleAdvanceStatus(entry) {
    const next = NEXT_STATUS[entry.status]
    if (!next) return
    try {
      await dlqApi.updateStatus(entry.id, next)
      refresh()
    } catch (err) {
      setActionError(getErrorMessage(err))
    }
  }

  async function handleExport() {
    setActionError('')
    setExporting(true)
    try {
      const params = isCustom ? { from: customFrom, to: customTo } : { dateRange: exportRange }
      // The export honours the same status filter that's applied to the list.
      // ALL means "no status param", i.e. every status in the window.
      if (status !== 'ALL') params.status = status
      const blob = await dlqApi.export(params)

      const rangeLabel = isCustom
        ? `custom_${customFrom.replace(/-/g, '')}_${customTo.replace(/-/g, '')}`
        : `${exportRange.toLowerCase()}_${todayIso().replace(/-/g, '')}`
      const statusLabel = status === 'ALL' ? '' : `${status.toLowerCase()}_`
      const filename = `dlq_report_${statusLabel}${rangeLabel}.xlsx`

      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      // Export responses are blobs, so error bodies arrive as blobs too.
      setActionError(await getErrorMessageAsync(err))
    } finally {
      setExporting(false)
    }
  }

  return (
    <div>
      <PageHeader title="Dead Letter Queue" description="Tasks that exhausted all retry attempts and require manual investigation" />
      <ErrorBanner message={error || actionError || customError} />

      <div className="mb-4 flex flex-wrap items-start gap-3">
        <div className="w-40">
          <Field label="Filter by status" hint="Applies to the list and the export">
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </Select>
          </Field>
        </div>

        <div className="w-44">
          <Field label="Export range">
            <Select value={exportRange} onChange={(e) => setExportRange(e.target.value)}>
              {EXPORT_RANGES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </Select>
          </Field>
        </div>

        {isCustom && (
          <>
            <div className="w-44">
              <Field label="From">
                <Input
                  type="date"
                  value={customFrom}
                  max={customTo || todayIso()}
                  onChange={(e) => setCustomFrom(e.target.value)}
                />
              </Field>
            </div>
            <div className="w-44">
              <Field label="To">
                <Input
                  type="date"
                  value={customTo}
                  min={customFrom || undefined}
                  max={todayIso()}
                  onChange={(e) => setCustomTo(e.target.value)}
                />
              </Field>
            </div>
          </>
        )}

        <div className="pt-[26px]">
          <PrimaryButton onClick={handleExport} disabled={!canExport}>
            <Download size={16} />
            {exporting ? 'Exporting...' : `Download ${status === 'ALL' ? 'XLSX' : status}`}
          </PrimaryButton>
        </div>
      </div>

      {loading && entries.length === 0 ? <LoadingState /> : entries.length === 0 ? (
        <EmptyState title="No DLQ entries" description="No tasks have failed all retry attempts." />
      ) : (
        <>
          <div className="space-y-3">
            {entries.map((entry) => (
              <div
                key={entry.id}
                className="rounded-xl border bg-[color:var(--color-surface)] p-4"
                style={{
                  borderColor: entry.status === 'FIXED'
                    ? 'var(--color-status-success)'
                    : entry.status === 'NEW'
                    ? 'var(--color-status-failed)'
                    : 'var(--color-border-soft)',
                }}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{entry.task?.name || `Task #${entry.taskId}`}</span>
                      <StatusBadge status={entry.status} />
                      <span className="font-[var(--font-mono)] text-xs text-[color:var(--color-ink-faint)]">
                        Execution #{entry.taskExecution?.id ?? '—'}
                      </span>
                    </div>
                    <p className="mt-2 text-sm text-[color:var(--color-status-failed)] bg-[color:var(--color-status-failed-soft)] rounded-md px-3 py-1.5 font-[var(--font-mono)] break-all">
                      {entry.failureReason}
                    </p>
                    <p className="mt-2 text-xs text-[color:var(--color-ink-muted)] font-[var(--font-mono)]">
                      Entered DLQ: {new Date(entry.createdAt).toLocaleString()}
                    </p>
                  </div>
                  {NEXT_STATUS[entry.status] && (
                    <button
                      onClick={() => handleAdvanceStatus(entry)}
                      className="shrink-0 rounded-lg border border-[color:var(--color-status-running)] px-3 py-1.5 text-xs font-medium text-[color:var(--color-status-running)] hover:bg-[color:var(--color-status-running-soft)] transition-colors"
                    >
                      Mark as {NEXT_STATUS[entry.status]}
                    </button>
                  )}
                </div>
              </div>
            ))}
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
    </div>
  )
}
