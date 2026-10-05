import { useEffect, useState, useCallback } from 'react'
import { Plus, Loader2, ChevronLeft, ChevronRight } from 'lucide-react'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import { fetchOrders, createOrder } from '../api/client'
import type { Order, Page } from '../types'

const STATUS_COLORS: Record<string, string> = {
  CREATED:         'text-text-muted bg-white/5 border-white/10',
  PAYMENT_PENDING: 'text-attention bg-attention/5 border-attention/20',
  PAID:            'text-healthy bg-healthy/5 border-healthy/20',
  FAILED:          'text-critical bg-critical/5 border-critical/20',
}

export default function OrdersPage() {
  const [data, setData] = useState<Page<Order> | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Create order form
  const [showForm, setShowForm] = useState(false)
  const [userId, setUserId] = useState('user-alice')
  const [amount, setAmount] = useState('99.99')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      setData(await fetchOrders(page, 20))
    } catch {
      setError('Failed to load orders')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { load() }, [load])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    const amountNum = parseFloat(amount)
    if (!userId.trim() || isNaN(amountNum) || amountNum <= 0) {
      setCreateError('Please provide a valid userId and amount > 0')
      return
    }
    setCreating(true)
    setCreateError(null)
    try {
      await createOrder(userId.trim(), amountNum)
      setShowForm(false)
      setUserId('user-alice')
      setAmount('99.99')
      await load()
    } catch (e: unknown) {
      setCreateError('Failed to create order')
    } finally {
      setCreating(false)
    }
  }

  if (loading) return <LoadingSpinner message="Loading orders..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  const orders = data?.content ?? []
  const total = data?.totalElements ?? 0

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-text-main">Orders</h1>
          <p className="text-sm text-text-muted">{total.toLocaleString()} orders in PostgreSQL</p>
        </div>
        <div className="flex gap-2">
          <button onClick={load} className="btn-secondary text-xs px-3 py-1.5">Refresh</button>
          <button onClick={() => setShowForm(!showForm)} className="btn-primary flex items-center gap-1.5 text-sm" id="create-order-btn">
            <Plus size={14} /> New Order
          </button>
        </div>
      </div>

      {/* Create order form */}
      {showForm && (
        <form onSubmit={handleCreate} className="card space-y-3 max-w-md">
          <h2 className="text-sm font-semibold text-text-main">Create Order</h2>
          <div>
            <label className="text-xs text-text-muted block mb-1">User ID</label>
            <input
              type="text"
              value={userId}
              onChange={e => setUserId(e.target.value)}
              className="w-full bg-elevated border border-white/10 rounded px-3 py-2 text-sm text-text-main placeholder-text-muted focus:outline-none focus:border-primary/50"
              placeholder="user-alice"
              id="order-user-id-input"
            />
          </div>
          <div>
            <label className="text-xs text-text-muted block mb-1">Amount (USD)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              className="w-full bg-elevated border border-white/10 rounded px-3 py-2 text-sm text-text-main focus:outline-none focus:border-primary/50"
              id="order-amount-input"
            />
          </div>
          {createError && <p className="text-xs text-critical">{createError}</p>}
          <div className="flex gap-2">
            <button type="submit" disabled={creating} className="btn-primary flex items-center gap-1.5 text-sm" id="submit-order-btn">
              {creating ? <><Loader2 size={13} className="animate-spin" />Creating...</> : 'Create Order'}
            </button>
            <button type="button" onClick={() => setShowForm(false)} className="btn-secondary text-sm">Cancel</button>
          </div>
        </form>
      )}

      <div className="card p-0 overflow-hidden">
        <table className="w-full">
          <thead className="border-b border-white/5 bg-elevated">
            <tr>
              <th className="table-th">Order ID</th>
              <th className="table-th">User</th>
              <th className="table-th text-right">Amount</th>
              <th className="table-th">Status</th>
              <th className="table-th">Created</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 && (
              <tr><td colSpan={5} className="table-td text-center text-text-muted py-8">
                No orders yet — create one or generate traffic
              </td></tr>
            )}
            {orders.map(o => (
              <tr key={o.id} className="table-row">
                <td className="table-td font-mono text-xs text-secondary">{o.id.substring(0, 8)}…</td>
                <td className="table-td text-xs">{o.userId}</td>
                <td className="table-td text-right font-mono text-sm">${Number(o.amount).toFixed(2)}</td>
                <td className="table-td">
                  <span className={`text-[10px] font-semibold uppercase px-2 py-0.5 rounded border ${STATUS_COLORS[o.status] ?? ''}`}>
                    {o.status.replace(/_/g, ' ')}
                  </span>
                </td>
                <td className="table-td text-xs text-text-muted">{formatDate(o.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

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

function formatDate(ts: string) {
  return new Date(ts).toLocaleString('en-US', { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })
}
