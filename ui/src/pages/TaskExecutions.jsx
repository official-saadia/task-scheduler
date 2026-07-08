import { useState, useCallback } from 'react'
import { executionsApi } from '../api/executions'
import { getErrorMessage } from '../api/client'
import { useServerPagination } from '../components/useServerPagination'
import { PageHeader, LoadingState, EmptyState } from '../components/PageElements'
import StatusBadge from '../components/StatusBadge'
import Pagination from '../components/Pagination'
import { Select, Field, ErrorBanner } from '../components/FormControls'

const STATUS_OPTIONS = ['ALL', 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'PARTIAL_SUCCESS', 'FAILED']

export default function TaskExecutions() {
  const [status, setStatus] = useState('ALL')

  const fetchFn = useCallback(
    (page, size) =>
      status === 'ALL'
        ? executionsApi.getAll(page, size)
        : executionsApi.getByStatus(status, page, size),
    [status]
  )

  const { data: executions, loading, error, ...pagination } = useServerPagination(fetchFn, 15, [status])

  return (
    <div>
      <PageHeader title="Task Executions" description="History of every time the scheduler engine ran a task, including retries" />
      <ErrorBanner message={error} />

      <div className="mb-4 max-w-xs">
        <Field label="Filter by status">
          <Select value={status} onChange={(e) => setStatus(e.target.value)}>
            {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
          </Select>
        </Field>
      </div>

      {loading && executions.length === 0 ? <LoadingState /> : executions.length === 0 ? (
        <EmptyState title="No executions found" description="Try a different status filter." />
      ) : (
        <>
          <div className="overflow-x-auto rounded-xl border border-[color:var(--color-border-soft)]">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] text-left text-xs uppercase text-[color:var(--color-ink-muted)]">
                  <th className="px-4 py-3 font-medium">ID</th>
                  <th className="px-4 py-3 font-medium">Task</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium">Retries</th>
                  <th className="px-4 py-3 font-medium">Started At</th>
                  <th className="px-4 py-3 font-medium">Completed At</th>
                </tr>
              </thead>
              <tbody>
                {executions.map((e) => (
                  <tr key={e.id} className="border-b border-[color:var(--color-border-soft)] last:border-0 hover:bg-[color:var(--color-surface)] transition-colors">
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs">#{e.id}</td>
                    <td className="px-4 py-3">{e.task?.name || `#${e.task?.id ?? '—'}`}</td>
                    <td className="px-4 py-3"><StatusBadge status={e.status} /></td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs">{e.retryCount}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs text-[color:var(--color-ink-muted)]">{formatDate(e.startedAt)}</td>
                    <td className="px-4 py-3 font-[var(--font-mono)] text-xs text-[color:var(--color-ink-muted)]">{formatDate(e.completedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            currentPage={pagination.currentPageDisplay}
            totalPages={pagination.totalPagesDisplay}
            totalItems={pagination.totalItems}
            pageSize={15}
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

function formatDate(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}
