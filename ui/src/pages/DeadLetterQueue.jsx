import { useState, useCallback } from 'react'
import { dlqApi } from '../api/executions'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import { Select, Field, ErrorBanner } from '../components/FormControls'

const STATUS_OPTIONS = ['ALL', 'NEW', 'IN_PROGRESS', 'ANALYSED', 'FIXED']
const NEXT_STATUS = { NEW: 'IN_PROGRESS', IN_PROGRESS: 'ANALYSED', ANALYSED: 'FIXED' }

export default function DeadLetterQueue() {
  const [status, setStatus] = useState('ALL')
  const [actionError, setActionError] = useState('')

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

  return (
    <div>
      <PageHeader title="Dead Letter Queue" description="Tasks that exhausted all retry attempts and require manual investigation" />
      <ErrorBanner message={error || actionError} />

      <div className="mb-4 max-w-xs">
        <Field label="Filter by status">
          <Select value={status} onChange={(e) => setStatus(e.target.value)}>
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
          </Select>
        </Field>
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
