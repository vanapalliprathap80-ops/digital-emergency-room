import { useEffect, useState, useCallback } from 'react'
import { RefreshCw, Search } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import MetricCard from '../components/MetricCard'
import StatusBadge from '../components/StatusBadge'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import { fetchMetrics, fetchServices, fetchIncidents } from '../api/client'
import type { SystemMetrics, ServiceStatus, IncidentResponse } from '../types'

export default function OverviewPage() {
  const navigate = useNavigate()
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null)
  const [services, setServices] = useState<ServiceStatus[]>([])
  const [activeIncidents, setActiveIncidents] = useState<IncidentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const [m, s, inc] = await Promise.all([
        fetchMetrics(), 
        fetchServices(),
        fetchIncidents(0, 50)
      ])
      setMetrics(m)
      setServices(s)
      setActiveIncidents(inc.content.filter(i => i.status === 'ACTIVE' || i.status === 'RECOVERING'))
    } catch (e: unknown) {
      setError('Failed to load overview data. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const interval = setInterval(load, 5000)
    return () => clearInterval(interval)
  }, [load])

  if (loading) return <LoadingSpinner message="Loading overview..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  const m = metrics!
  const allHealthy = services.every(s => s.status === 'HEALTHY') && activeIncidents.length === 0

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-text-main">System Overview</h1>
          <p className="text-sm text-text-muted mt-0.5">Real-time production simulator metrics</p>
        </div>
        <div className="flex items-center gap-3">
          <span className={`flex items-center gap-1.5 text-sm font-medium ${allHealthy ? 'text-healthy' : 'text-attention'}`}>
            <span className={`w-2 h-2 rounded-full ${allHealthy ? 'bg-healthy' : 'bg-attention'} animate-pulse`} />
            {allHealthy ? 'All Systems Operational' : 'Degraded State'}
          </span>
          <button onClick={load} className="btn-secondary flex items-center gap-1.5 px-3 py-1.5 text-xs">
            <RefreshCw size={12} />
            Refresh
          </button>
        </div>
      </div>

      {/* System Metrics */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
        <MetricCard label="Total Requests" value={m.totalRequests.toLocaleString()} accent="secondary" />
        <MetricCard label="Requests / min" value={m.requestsPerMinute.toFixed(0)} unit="rpm" accent="secondary" />
        <MetricCard
          label="Error Rate"
          value={m.errorRate.toFixed(2)}
          unit="%"
          accent={m.errorRate > 5 ? 'critical' : m.errorRate > 1 ? 'attention' : 'healthy'}
        />
        <MetricCard label="Avg Latency" value={m.averageLatencyMs.toFixed(0)} unit="ms" accent="secondary" />
        <MetricCard label="P95 Latency" value={m.p95LatencyMs} unit="ms" accent="secondary" />
      </div>

      {/* Active Incidents (Conditionally Rendered) */}
      {activeIncidents.length > 0 && (
        <div>
          <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wider mb-3">Active Incidents</h2>
          <div className="space-y-3">
            {activeIncidents.map(inc => (
              <div key={inc.incidentId} className="p-4 rounded-md border flex items-center justify-between bg-critical/10 border-critical/30">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-text-main">{inc.incidentId}</span>
                    <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-critical text-white">
                      {inc.status}
                    </span>
                    <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-white/10 text-text-muted">
                      {inc.mode}
                    </span>
                    <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-white/10 text-text-muted">
                      {inc.severity}
                    </span>
                  </div>
                  <div className="text-xs text-text-muted mt-1">
                    Target: <span className="text-text-main">{inc.affectedService || 'UNKNOWN'}</span> • 
                    Started: {new Date(inc.startedAt).toLocaleTimeString()}
                  </div>
                </div>
                <button
                  onClick={() => navigate(`/investigations/${inc.incidentId}`)}
                  className="btn-primary flex items-center gap-1.5 px-3 py-1.5 text-xs"
                >
                  <Search size={14} />
                  Investigate
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Service Health Grid */}
      <div>
        <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wider mb-3">Service Health</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          {services.map(svc => (
            <div key={svc.service} className="card flex flex-col gap-3">
              <div className="flex items-start justify-between">
                <span className="text-sm font-medium text-text-main leading-tight">
                  {formatServiceName(svc.service)}
                </span>
                <StatusBadge status={svc.status} showLabel={false} />
              </div>
              <StatusBadge status={svc.status} />
              <p className="text-[10px] text-text-muted leading-relaxed">{svc.description}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function formatServiceName(s: string) {
  return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}
