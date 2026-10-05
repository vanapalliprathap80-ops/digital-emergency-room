interface Props {
  label: string
  value: string | number
  unit?: string
  accent?: 'primary' | 'secondary' | 'healthy' | 'attention' | 'critical'
  sub?: string
}

const accentClass: Record<string, string> = {
  primary:   'text-primary',
  secondary: 'text-secondary',
  healthy:   'text-healthy',
  attention: 'text-attention',
  critical:  'text-critical',
}

export default function MetricCard({ label, value, unit, accent = 'secondary', sub }: Props) {
  return (
    <div className="card flex flex-col gap-1">
      <span className="metric-label">{label}</span>
      <div className="flex items-baseline gap-1">
        <span className={`metric-value ${accentClass[accent]}`}>{value}</span>
        {unit && <span className="text-xs text-text-muted">{unit}</span>}
      </div>
      {sub && <span className="text-xs text-text-muted">{sub}</span>}
    </div>
  )
}
