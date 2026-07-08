import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { analyticsApi } from '../api/analytics'
import { getErrorMessage } from '../api/client'
import { StatCard, PageHeader, LoadingState } from '../components/PageElements'
import { ErrorBanner } from '../components/FormControls'

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

  const chartData = data
    ? [
        {
          name: 'Executions',
          Completed: data.successfulExecutions,
          Failed: data.failedExecutions,
        },
        {
          name: 'Emails',
          Completed: data.successfulEmails,
          Failed: data.failedEmails,
        },
      ]
    : []

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Live overview of scheduled tasks, executions, and notifications"
      />

      <ErrorBanner message={error} />

      {data && (
        <>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <StatCard label="Total Tasks" value={data.totalTasks} accent="scheduled" />
            <StatCard label="Total Executions" value={data.totalExecutions} accent="running" />
            <StatCard
              label="Successful Executions"
              value={data.successfulExecutions}
              accent="success"
            />
            <StatCard label="Failed Executions" value={data.failedExecutions} accent="failed" />
            <StatCard label="Emails Sent" value={data.totalEmailsSent} accent="scheduled" />
            <StatCard
              label="Emails Delivered"
              value={data.successfulEmails}
              accent="success"
            />
            <StatCard label="Emails Failed" value={data.failedEmails} accent="failed" />
            <StatCard label="DLQ Entries" value={data.totalDlqEntries} accent="failed" />
          </div>

          <div className="mt-8 rounded-xl border border-[color:var(--color-border-soft)] bg-[color:var(--color-surface)] p-6">
            <h2 className="mb-4 text-base font-semibold">Success vs Failure</h2>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border-soft)" />
                <XAxis dataKey="name" stroke="var(--color-ink-muted)" fontSize={12} />
                <YAxis stroke="var(--color-ink-muted)" fontSize={12} allowDecimals={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'var(--color-surface-raised)',
                    border: '1px solid var(--color-border-soft)',
                    borderRadius: '8px',
                    fontSize: '13px',
                  }}
                />
                <Bar dataKey="Completed" fill="var(--color-status-success)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="Failed" fill="var(--color-status-failed)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </div>
  )
}
