import { useEffect, useState } from 'react'
import { analyticsApi } from '../api/analytics'
import { getErrorMessage } from '../api/client'
import { StatCard, PageHeader, LoadingState } from '../components/PageElements'
import { ErrorBanner } from '../components/FormControls'

const headingColor = { color: 'var(--color-heading)' }

function SectionHeading({ children }) {
  return (
    <h2 className="mb-3 text-base font-semibold" style={headingColor}>
      {children}
    </h2>
  )
}

function ExecutionStats({ stats }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <StatCard label="Executions" value={stats.totalExecutions} accent="running" />
      <StatCard label="Successful" value={stats.successfulExecutions} accent="success" />
      <StatCard label="Skipped" value={stats.skippedExecutions} accent="skipped" />
      <StatCard label="Failed" value={stats.failedExecutions} accent="failed" />
    </div>
  )
}

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    analyticsApi
      .get()
      .then(setData)
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  const periods = data
    ? [
        { label: 'Today', stats: data.today },
        { label: 'This Week', stats: data.thisWeek },
        { label: 'This Month', stats: data.thisMonth },
      ]
    : []

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Live overview of scheduled tasks and execution outcomes"
      />

      <ErrorBanner message={error} />

      {data && (
        <>
          <section>
            <SectionHeading>Overview</SectionHeading>
            <div className="grid grid-cols-2 gap-4">
              <StatCard label="Total Tasks" value={data.totalTasks} accent="scheduled" />
              <StatCard label="DLQ Entries" value={data.totalDlqEntries} accent="failed" />
            </div>
          </section>

          {periods.map(({ label, stats }) => (
            <section key={label} className="mt-6">
              <SectionHeading>{label}</SectionHeading>
              <ExecutionStats stats={stats} />
            </section>
          ))}
        </>
      )}
    </div>
  )
}
