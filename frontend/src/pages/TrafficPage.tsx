import { useState } from 'react'
import { Zap, CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { generateTraffic } from '../api/client'
import type { TrafficGenerateResponse } from '../types'

const PRESETS = [10, 50, 100, 500]

export default function TrafficPage() {
  const [selected, setSelected] = useState(10)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<TrafficGenerateResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleGenerate = async () => {
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const res = await generateTraffic(selected)
      setResult(res)
    } catch (e: unknown) {
      setError('Failed to generate traffic. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6 max-w-xl">
      <div>
        <h1 className="text-xl font-bold text-text-main">Traffic Generator</h1>
        <p className="text-sm text-text-muted mt-0.5">
          Executes real simulated requests through the complete service stack.
          Each generated request produces actual telemetry, events, and orders.
        </p>
      </div>

      <div className="card space-y-5">
        <div>
          <label className="text-xs font-medium text-text-muted uppercase tracking-wider block mb-2">
            Request Count
          </label>
          <div className="grid grid-cols-4 gap-2">
            {PRESETS.map(n => (
              <button
                key={n}
                onClick={() => setSelected(n)}
                className={`py-2 rounded-md text-sm font-semibold border transition-all ${
                  selected === n
                    ? 'bg-primary/15 border-primary/40 text-primary'
                    : 'bg-elevated border-white/10 text-text-muted hover:border-white/20 hover:text-text-main'
                }`}
              >
                {n}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleGenerate}
            disabled={loading}
            className="btn-primary flex items-center gap-2"
            id="generate-traffic-btn"
          >
            {loading
              ? <><Loader2 size={14} className="animate-spin" />Generating {selected} requests...</>
              : <><Zap size={14} />Generate {selected} Requests</>
            }
          </button>
        </div>

        {loading && (
          <div className="p-3 rounded bg-primary/5 border border-primary/10 text-xs text-text-muted">
            Executing {selected} requests through the full service stack (API Gateway → Auth → Orders → Payment → Notification)...
          </div>
        )}

        {error && (
          <div className="p-3 rounded bg-critical/5 border border-critical/20 text-xs text-critical">
            {error}
          </div>
        )}
      </div>

      {result && (
        <div className="card space-y-4">
          <h2 className="text-sm font-semibold text-text-main">Generation Complete</h2>

          <div className="grid grid-cols-2 gap-3">
            <Stat label="Requested" value={result.requested} />
            <Stat label="Completed" value={result.completed} />
            <Stat label="Succeeded" value={result.succeeded} color="text-healthy" icon={<CheckCircle size={14} className="text-healthy" />} />
            <Stat label="Failed" value={result.failed} color={result.failed > 0 ? 'text-critical' : 'text-text-muted'} icon={result.failed > 0 ? <XCircle size={14} className="text-critical" /> : undefined} />
          </div>

          <div className="pt-2 border-t border-white/5">
            <div className="flex items-center justify-between text-xs">
              <span className="text-text-muted">Duration</span>
              <span className="font-mono text-text-main">{result.durationMs}ms</span>
            </div>
            <div className="flex items-center justify-between text-xs mt-1">
              <span className="text-text-muted">Throughput</span>
              <span className="font-mono text-text-main">
                {result.durationMs > 0 ? ((result.completed / result.durationMs) * 1000).toFixed(1) : '—'} req/s
              </span>
            </div>
          </div>

          <p className="text-[10px] text-text-muted">
            Check the Overview, Requests, Events, and Orders pages to see the generated activity.
          </p>
        </div>
      )}
    </div>
  )
}

function Stat({ label, value, color = 'text-text-main', icon }: {
  label: string; value: number; color?: string; icon?: React.ReactNode
}) {
  return (
    <div className="bg-elevated rounded-md p-3">
      <div className="text-[10px] text-text-muted uppercase tracking-wider mb-1">{label}</div>
      <div className={`text-xl font-bold tabular-nums flex items-center gap-1.5 ${color}`}>
        {icon}{value.toLocaleString()}
      </div>
    </div>
  )
}
