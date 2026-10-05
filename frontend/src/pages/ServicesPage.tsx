import { useEffect, useState, useCallback } from 'react'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import { fetchServiceMetrics } from '../api/client'
import type { ServiceMetrics } from '../types'

function fmt(n: number, d = 0) { return n.toFixed(d) }

export default function ServicesPage() {
  const [services, setServices] = useState<ServiceMetrics[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<ServiceMetrics | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const data = await fetchServiceMetrics()
      setServices(data)
    } catch {
      setError('Failed to load service metrics')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, 5000)
    return () => clearInterval(id)
  }, [load])

  if (loading) return <LoadingSpinner message="Loading services..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-text-main">Services</h1>
        <p className="text-sm text-text-muted mt-0.5">Per-service metrics computed from real telemetry</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {services.map(svc => (
          <button
            key={svc.service}
            onClick={() => setSelected(selected?.service === svc.service ? null : svc)}
            className={`card text-left transition-all ${
              selected?.service === svc.service ? 'border-primary/40 bg-primary/5' : 'hover:border-white/10'
            }`}
          >
            <div className="flex items-center justify-between mb-3">
              <span className="font-semibold text-text-main">{formatServiceName(svc.service)}</span>
              <StatusBadge status={svc.healthStatus} />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <Stat label="Requests" value={svc.requestCount.toLocaleString()} />
              <Stat label="Errors" value={svc.errorCount.toLocaleString()} accent={svc.errorCount > 0} />
              <Stat label="Error Rate" value={`${fmt(svc.errorRate, 2)}%`} accent={svc.errorRate > 1} />
              <Stat label="Avg Latency" value={`${fmt(svc.averageLatencyMs, 0)}ms`} />
              <Stat label="P95 Latency" value={`${svc.p95LatencyMs}ms`} />
              <Stat label="Req/min" value={fmt(svc.requestsPerMinute, 0)} />
            </div>
          </button>
        ))}
      </div>

      {selected && (
        <div className="card border-primary/30">
          <h3 className="text-sm font-semibold text-primary mb-3 uppercase tracking-wider">
            {formatServiceName(selected.service)} — Detail
          </h3>
          <div className="grid grid-cols-3 md:grid-cols-6 gap-4 text-center">
            <DetailStat label="Health" value={<StatusBadge status={selected.healthStatus} />} />
            <DetailStat label="Total Requests" value={selected.requestCount.toLocaleString()} />
            <DetailStat label="Errors" value={selected.errorCount.toLocaleString()} />
            <DetailStat label="Error Rate" value={`${fmt(selected.errorRate, 2)}%`} />
            <DetailStat label="Avg Latency" value={`${fmt(selected.averageLatencyMs, 0)} ms`} />
            <DetailStat label="P95 Latency" value={`${selected.p95LatencyMs} ms`} />
          </div>
        </div>
      )}
    </div>
  )
}

function Stat({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div>
      <div className="text-[10px] text-text-muted uppercase">{label}</div>
      <div className={`text-sm font-medium ${accent ? 'text-critical' : 'text-text-main'}`}>{value}</div>
    </div>
  )
}

function DetailStat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div className="text-[10px] text-text-muted uppercase tracking-wide">{label}</div>
      <div className="text-sm font-medium text-text-main">{value}</div>
    </div>
  )
}

function formatServiceName(s: string) {
  return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}
