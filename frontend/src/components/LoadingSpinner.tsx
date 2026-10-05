import { Loader2 } from 'lucide-react'

interface Props { message?: string }

export default function LoadingSpinner({ message = 'Loading...' }: Props) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-text-muted">
      <Loader2 className="animate-spin" size={20} />
      <span className="text-sm">{message}</span>
    </div>
  )
}
