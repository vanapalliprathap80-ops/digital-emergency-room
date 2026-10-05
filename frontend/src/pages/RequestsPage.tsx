import { useEffect, useState, useCallback } from 'react'
import { CheckCircle, XCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import { fetchRequests } from '../api/client'
import type { TelemetryRecord, Page } from '../types'

const SERVICES = ['API_GATEWAY','AUTH','ORDERS','PAYMENT','DATABASE','NOTIFICATION']

export default function RequestsPage() {
  const [data, setData] = useState<Page<TelemetryRecord> | null>(null)
  const [page, setPage] = useState(0)
  const [service, setService] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const result = await fetchRequests(page, 50, service || undefined)
      setData(result)
    } catch {
      setError('Failed to load requests')
    } finally {
      setLoading(false)
    }
  }, [page, service])

  useEffect(() => { load() }, [load])

  const changeService = (s: string) => { setService(s); setPage(0) }

  if (loading) return <LoadingSpinner message="Loading requests..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  const rows = data?.content ?? []
  const total = data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-text-main">Requests</h1>
          <p className="text-sm text-text-muted">{total.toLocaleString()} telemetry spans recorded</p>
        </div>
        <button onClick={load} className="btn-secondary text-xs px-3 py-1.5">Refresh</button>
      </div>

      {/* Service filter */}
      <div className="flex gap-2 flex-wrap">
        <button
          onClick={() => changeService('')}
          className={`px-3 py-1 rounded text-xs font-medium border transition-colors ${
            !service ? 'bg-primary/10 text-primary border-primary/30' : 'text-text-muted border-white/10 hover:border-white/20'
          }`}
        >All</button>
        {SERVICES.map(s => (
          <button
            key={s}
            onClick={() => changeService(s)}
            className={`px-3 py-1 rounded text-xs font-medium border transition-colors ${
              service === s ? 'bg-primary/10 text-primary border-primary/30' : 'text-text-muted border-white/10 hover:border-white/20'
            }`}
          >
            {s.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="card p-0 overflow-hidden">
        <table className="w-full">
          <thead className="border-b border-white/5 bg-elevated">
            <tr>
              <th className="table-th">Time</th>
              <th className="table-th">Request ID</th>
              <th className="table-th">Service</th>
              <th className="table-th">Operation</th>
              <th className="table-th">Status</th>
              <th className="table-th text-right">Latency</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan={6} className="table-td text-center text-text-muted py-8">No requests yet — generate some traffic</td></tr>
            )}
            {rows.map(r => (
              <tr key={r.id} className="table-row">
                <td className="table-td font-mono text-xs text-text-muted">{formatTime(r.timestamp)}</td>
                <td className="table-td font-mono text-xs text-secondary">{r.requestId}</td>
                <td className="table-td">
                  <span className="text-xs px-2 py-0.5 rounded bg-white/5 text-text-muted border border-white/5">
                    {r.service}
                  </span>
                </td>
                <td className="table-td text-xs text-text-muted">{r.operation}</td>
                <td className="table-td">
                  {r.success
                    ? <CheckCircle size={14} className="text-healthy" />
                    : <XCircle size={14} className="text-critical" />}
                </td>
                <td className="table-td text-right font-mono text-xs text-text-muted">{r.latencyMs}ms</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-text-muted">
          <span>Page {page + 1} of {data.totalPages}</span>
          <div className="flex gap-2">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="btn-secondary p-1.5 disabled:opacity-30">
              <ChevronLeft size={14} />
            </button>
            <button disabled={data.last} onClick={() => setPage(p => p + 1)} className="btn-secondary p-1.5 disabled:opacity-30">
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
