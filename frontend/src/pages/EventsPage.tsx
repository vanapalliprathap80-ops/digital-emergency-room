import { useEffect, useState, useCallback } from 'react'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import { fetchEvents } from '../api/client'
import type { ApplicationEvent, Page } from '../types'

const EVENT_COLORS: Record<string, string> = {
  ORDER_CREATED:         'text-secondary border-secondary/20 bg-secondary/5',
  PAYMENT_PROCESSED:     'text-healthy border-healthy/20 bg-healthy/5',
  PAYMENT_FAILED:        'text-critical border-critical/20 bg-critical/5',
  NOTIFICATION_SENT:     'text-primary border-primary/20 bg-primary/5',
  AUTH_FAILURE:          'text-attention border-attention/20 bg-attention/5',
  REQUEST_FAILED:        'text-critical border-critical/20 bg-critical/5',
}

export default function EventsPage() {
  const [data, setData] = useState<Page<ApplicationEvent> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      const result = await fetchEvents(0, 100)
      setData(result)
    } catch {
      setError('Failed to load events')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const id = setInterval(load, 5000)
    return () => clearInterval(id)
  }, [load])

  if (loading) return <LoadingSpinner message="Loading events..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  const events = data?.content ?? []

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-text-main">Events</h1>
          <p className="text-sm text-text-muted">{(data?.totalElements ?? 0).toLocaleString()} application events</p>
        </div>
        <button onClick={load} className="btn-secondary text-xs px-3 py-1.5">Refresh</button>
      </div>

      <div className="card p-0 divide-y divide-white/5">
        {events.length === 0 && (
          <p className="text-center text-text-muted text-sm py-8">No events yet — create an order or generate traffic</p>
        )}
        {events.map(evt => (
          <div key={evt.id} className="flex items-start gap-4 px-4 py-3 hover:bg-white/2 transition-colors">
            <span className="text-xs font-mono text-text-muted pt-0.5 w-20 shrink-0">
              {formatTime(evt.timestamp)}
            </span>
            <span className={`inline-block text-[10px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded border shrink-0 ${EVENT_COLORS[evt.eventType] ?? 'text-text-muted border-white/10 bg-white/5'}`}>
              {evt.eventType.replace(/_/g, ' ')}
            </span>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-text-main leading-snug">{evt.message}</p>
              {evt.requestId && (
                <p className="text-[10px] font-mono text-text-muted mt-0.5">{evt.requestId}</p>
              )}
            </div>
            <span className="text-[10px] text-text-muted shrink-0">{evt.service}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' })
}
