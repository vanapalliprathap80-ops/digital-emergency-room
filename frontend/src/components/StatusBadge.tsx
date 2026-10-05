import type { HealthStatus } from '../types'

interface Props {
  status: HealthStatus
  showLabel?: boolean
  size?: 'sm' | 'md'
}

const statusConfig: Record<HealthStatus, { dot: string; badge: string; label: string }> = {
  HEALTHY:     { dot: 'bg-healthy', badge: 'badge-healthy', label: 'Healthy' },
  DEGRADED:    { dot: 'bg-attention', badge: 'badge-degraded', label: 'Degraded' },
  UNAVAILABLE: { dot: 'bg-critical', badge: 'badge-unavailable', label: 'Unavailable' },
}

export default function StatusBadge({ status, showLabel = true, size = 'md' }: Props) {
  const cfg = statusConfig[status] ?? statusConfig.HEALTHY
  const dotSize = size === 'sm' ? 'w-1.5 h-1.5' : 'w-2 h-2'
  return (
    <span className={cfg.badge}>
      <span className={`inline-block ${dotSize} rounded-full ${cfg.dot} animate-pulse`} />
      {showLabel && cfg.label}
    </span>
  )
}
