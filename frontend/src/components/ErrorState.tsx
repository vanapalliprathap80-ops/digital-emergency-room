import { AlertCircle } from 'lucide-react'

interface Props { message?: string; onRetry?: () => void }

export default function ErrorState({ message = 'Failed to load data', onRetry }: Props) {
  return (
    <div className="flex flex-col items-center gap-3 py-12 text-text-muted">
      <AlertCircle size={28} className="text-critical" />
      <p className="text-sm">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary text-xs px-3 py-1.5">
          Retry
        </button>
      )}
    </div>
  )
}
