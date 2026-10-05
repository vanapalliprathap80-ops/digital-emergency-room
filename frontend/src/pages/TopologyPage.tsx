import { useEffect, useState, useCallback } from 'react'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorState from '../components/ErrorState'
import StatusBadge from '../components/StatusBadge'
import { fetchTopology } from '../api/client'
import type { TopologyResponse, TopologyNode } from '../types'

// Column layout for the 6 nodes:
//   Col 0: API_GATEWAY
//   Col 1: AUTH, ORDERS
//   Col 2: DATABASE, PAYMENT
//   Col 3: NOTIFICATION
const LAYOUT: Record<string, { col: number; row: number }> = {
  API_GATEWAY:  { col: 0, row: 0 },
  AUTH:         { col: 1, row: 0 },
  ORDERS:       { col: 1, row: 1 },
  DATABASE:     { col: 2, row: 0 },
  PAYMENT:      { col: 2, row: 1 },
  NOTIFICATION: { col: 3, row: 0 },
}

const COL_WIDTH = 180
const ROW_HEIGHT = 90
const NODE_W = 140
const NODE_H = 60
const PAD_X = 20
const PAD_Y = 30

function nodeX(col: number) { return PAD_X + col * COL_WIDTH + NODE_W / 2 }
function nodeY(row: number) { return PAD_Y + row * ROW_HEIGHT + NODE_H / 2 }

export default function TopologyPage() {
  const [topology, setTopology] = useState<TopologyResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setError(null)
      setTopology(await fetchTopology())
    } catch {
      setError('Failed to load topology')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  if (loading) return <LoadingSpinner message="Loading topology..." />
  if (error)   return <ErrorState message={error} onRetry={load} />

  const { nodes, edges } = topology!
  const nodeMap = Object.fromEntries(nodes.map(n => [n.id, n]))

  const svgW = PAD_X * 2 + 4 * COL_WIDTH
  const svgH = PAD_Y * 2 + 2 * ROW_HEIGHT

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-text-main">Service Topology</h1>
        <p className="text-sm text-text-muted mt-0.5">
          Backend-authoritative dependency graph — health status reflects live state
        </p>
      </div>

      <div className="card overflow-x-auto">
        <svg viewBox={`0 0 ${svgW} ${svgH}`} width="100%" style={{ minWidth: svgW }}>
          <defs>
            <marker id="arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" fill="#22D3EE" opacity="0.6" />
            </marker>
          </defs>

          {/* Edges */}
          {edges.map((edge, i) => {
            const src = LAYOUT[edge.source]
            const tgt = LAYOUT[edge.target]
            if (!src || !tgt) return null
            const x1 = nodeX(src.col)
            const y1 = nodeY(src.row)
            const x2 = nodeX(tgt.col)
            const y2 = nodeY(tgt.row)
            return (
              <g key={i}>
                <line
                  x1={x1} y1={y1} x2={x2} y2={y2}
                  stroke="#22D3EE" strokeOpacity={0.3} strokeWidth={1.5}
                  markerEnd="url(#arrow)"
                />
                <text
                  x={(x1 + x2) / 2} y={(y1 + y2) / 2 - 4}
                  textAnchor="middle" fill="#94A3B8" fontSize={8}
                >
                  {edge.label}
                </text>
              </g>
            )
          })}

          {/* Nodes */}
          {nodes.map(node => {
            const pos = LAYOUT[node.id]
            if (!pos) return null
            const cx = nodeX(pos.col) - NODE_W / 2
            const cy = nodeY(pos.row) - NODE_H / 2
            const isHealthy = node.healthStatus === 'HEALTHY'
            const strokeColor = isHealthy ? '#34D399' : node.healthStatus === 'DEGRADED' ? '#F59E0B' : '#F43F5E'

            return (
              <g key={node.id}>
                <rect
                  x={cx} y={cy} width={NODE_W} height={NODE_H}
                  rx={6} ry={6}
                  fill="#151C30" stroke={strokeColor} strokeWidth={1.5} strokeOpacity={0.6}
                />
                <text x={cx + NODE_W / 2} y={cy + NODE_H / 2 - 8}
                  textAnchor="middle" fill="#F8FAFC" fontSize={11} fontWeight={600}>
                  {node.label}
                </text>
                <text x={cx + NODE_W / 2} y={cy + NODE_H / 2 + 8}
                  textAnchor="middle" fill={strokeColor} fontSize={9}>
                  ● {node.healthStatus}
                </text>
              </g>
            )
          })}
        </svg>
      </div>

      {/* Legend */}
      <div className="flex gap-4 text-xs text-text-muted">
        <span className="flex items-center gap-1.5"><span className="w-3 h-0.5 bg-healthy/60 inline-block" />Healthy</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-0.5 bg-attention/60 inline-block" />Degraded</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-0.5 bg-critical/60 inline-block" />Unavailable</span>
        <span className="flex items-center gap-1.5"><span className="w-3 h-0.5 bg-secondary/40 inline-block" />Dependency</span>
      </div>

      {/* Node detail list */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        {nodes.map(n => (
          <div key={n.id} className="card flex items-center justify-between">
            <div>
              <div className="text-sm font-medium text-text-main">{n.label}</div>
              {n.dependsOn.length > 0 && (
                <div className="text-[10px] text-text-muted mt-0.5">
                  depends on: {n.dependsOn.join(', ')}
                </div>
              )}
            </div>
            <StatusBadge status={n.healthStatus} />
          </div>
        ))}
      </div>
    </div>
  )
}
