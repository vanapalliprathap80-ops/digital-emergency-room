import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { AlertOctagon, RefreshCw, Zap, ShieldAlert, Play, RotateCcw, Search } from 'lucide-react'
import {
  fetchSystemState,
  fetchIncidents,
  createManualIncident,
  createChaosIncident,
  runChaosSuite,
  resetIncident,
} from '../api/client'
import type { SimulatedSystemStateDto, IncidentResponse, LogicalService, IncidentMode, Page } from '../types'
import MetricCard from '../components/MetricCard'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'

const SERVICES: LogicalService[] = ['API_GATEWAY', 'AUTH', 'ORDERS', 'PAYMENT', 'DATABASE', 'NOTIFICATION']

export default function ChaosPage() {
  const navigate = useNavigate()
  const [state, setState] = useState<SimulatedSystemStateDto | null>(null)
  const [incidents, setIncidents] = useState<IncidentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [injecting, setInjecting] = useState(false)

  const load = useCallback(async () => {
    try {
      setError(null)
      const [st, inc] = await Promise.all([
        fetchSystemState(),
        fetchIncidents(0, 10)
      ])
      setState(st)
      setIncidents(inc.content)
    } catch (e: unknown) {
      setError('Failed to load chaos state.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const interval = setInterval(load, 3000)
    return () => clearInterval(interval)
  }, [load])

  const handleManualInject = async (service: LogicalService) => {
    try {
      setInjecting(true)
      await createManualIncident(service, 'MANUAL')
      await load()
    } catch (e) {
      console.error(e)
    } finally {
      setInjecting(false)
    }
  }

  const handleChaosInject = async () => {
    try {
      setInjecting(true)
      await createChaosIncident()
      await load()
    } catch (e) {
      console.error(e)
    } finally {
      setInjecting(false)
    }
  }

  const handleRunSuite = async () => {
    try {
      setInjecting(true)
      await runChaosSuite(3) // Run 3 iterations for demo
      await load()
    } catch (e) {
      console.error(e)
    } finally {
      setInjecting(false)
    }
  }

  const handleReset = async (id: string) => {
    try {
      setInjecting(true)
      await resetIncident(id)
      await load()
    } catch (e) {
      console.error(e)
    } finally {
      setInjecting(false)
    }
  }

  if (loading && !state) return <LoadingSpinner message="Loading chaos engine..." />
  if (error) return <ErrorState message={error} onRetry={load} />

  const s = state!
  const activeIncidents = incidents.filter(i => i.status === 'ACTIVE')

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-text-main flex items-center gap-2">
            <AlertOctagon size={20} className="text-critical" />
            Chaos Engine
          </h1>
          <p className="text-sm text-text-muted mt-0.5">Failure injection and resilience testing (Phase 2)</p>
        </div>
        <div className="flex items-center gap-3">
          {s.hasActiveIncident && (
            <span className="flex items-center gap-1.5 text-sm font-medium text-critical">
              <span className="w-2 h-2 rounded-full bg-critical animate-pulse" />
              Incident Active
            </span>
          )}
          <button onClick={load} className="btn-secondary flex items-center gap-1.5 px-3 py-1.5 text-xs">
            <RefreshCw size={12} />
            Refresh
          </button>
        </div>
      </div>

      {/* State Metrics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <MetricCard label="CPU Utilization" value={s.cpuUtilizationPercent.toFixed(1)} unit="%" accent={s.cpuUtilizationPercent > 80 ? 'critical' : 'secondary'} />
        <MetricCard label="Memory Utilization" value={s.memoryUtilizationPercent.toFixed(1)} unit="%" accent={s.memoryUtilizationPercent > 80 ? 'critical' : 'secondary'} />
        <MetricCard label="DB Active Conns" value={s.dbActiveConnections.toString()} accent={s.dbAvailable ? 'secondary' : 'critical'} />
        <MetricCard label="DB Acq Failures" value={s.dbAcquisitionFailures.toString()} accent={s.dbAcquisitionFailures > 0 ? 'critical' : 'healthy'} />
      </div>

      {/* Controls */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Manual Injection */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2">
            <ShieldAlert size={16} className="text-attention" />
            <h2 className="text-sm font-semibold text-text-main">Manual Injection</h2>
          </div>
          <p className="text-xs text-text-muted">Target specific logical services to intentionally degrade or fail them.</p>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-2">
            {SERVICES.map(svc => (
              <button
                key={svc}
                onClick={() => handleManualInject(svc)}
                disabled={injecting || s.hasActiveIncident}
                className="btn-secondary py-1.5 px-2 text-xs text-left justify-start truncate disabled:opacity-50"
              >
                Fail {svc.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        {/* Chaos Injection */}
        <div className="card space-y-4">
          <div className="flex items-center gap-2">
            <Zap size={16} className="text-primary" />
            <h2 className="text-sm font-semibold text-text-main">Autonomous Chaos</h2>
          </div>
          <p className="text-xs text-text-muted">Let the Chaos Engine randomly select a failure mode and target service based on a random seed.</p>
          <div className="flex gap-3">
            <button
              onClick={handleChaosInject}
              disabled={injecting || s.hasActiveIncident}
              className="btn-primary flex items-center gap-1.5 py-1.5 px-3 text-xs disabled:opacity-50"
            >
              <Zap size={14} />
              Inject Random Chaos
            </button>
            <button
              onClick={handleRunSuite}
              disabled={injecting || s.hasActiveIncident}
              className="btn-secondary flex items-center gap-1.5 py-1.5 px-3 text-xs disabled:opacity-50"
            >
              <Play size={14} />
              Run Test Suite (3x)
            </button>
          </div>
        </div>
      </div>

      {/* Active / Recent Incidents */}
      <div>
        <h2 className="text-sm font-semibold text-text-muted uppercase tracking-wider mb-3">Active & Recent Incidents</h2>
        {incidents.length === 0 ? (
          <div className="text-sm text-text-muted p-4 border border-white/10 rounded-md text-center">
            No incidents recorded in this session.
          </div>
        ) : (
          <div className="space-y-3">
            {incidents.map(inc => (
              <div key={inc.incidentId} className={`p-4 rounded-md border flex items-center justify-between ${inc.status === 'ACTIVE' ? 'bg-critical/10 border-critical/30' : 'bg-surface border-white/5'}`}>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-text-main">{inc.incidentId}</span>
                    <span className={`text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded ${inc.status === 'ACTIVE' ? 'bg-critical text-white' : 'bg-white/10 text-text-muted'}`}>
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
                    {inc.endedAt && ` • Ended: ${new Date(inc.endedAt).toLocaleTimeString()}`}
                  </div>
                </div>
                {inc.status === 'ACTIVE' && (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => navigate(`/investigations/${inc.incidentId}`)}
                      className="btn-primary flex items-center gap-1.5 px-3 py-1.5 text-xs"
                    >
                      <Search size={14} />
                      Investigate
                    </button>
                    <button
                      onClick={() => handleReset(inc.incidentId)}
                      disabled={injecting}
                      className="btn-secondary flex items-center gap-1.5 px-3 py-1.5 text-xs text-healthy border-healthy hover:bg-healthy/10"
                    >
                      <RotateCcw size={14} />
                      Resolve & Reset
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
