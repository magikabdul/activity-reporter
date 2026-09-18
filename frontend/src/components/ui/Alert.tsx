import { CircleAlert, CircleCheck, Info } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

type Tone = 'danger' | 'success' | 'info'

const TONES: Record<Tone, { box: string; icon: typeof Info }> = {
  danger: { box: 'border-danger/40 bg-danger-soft text-danger', icon: CircleAlert },
  success: { box: 'border-success/40 bg-success-soft text-success', icon: CircleCheck },
  info: { box: 'border-accent/30 bg-accent-soft text-accent', icon: Info },
}

interface AlertProps {
  tone?: Tone
  title: string
  children?: ReactNode
  className?: string
}

export function Alert({ tone = 'info', title, children, className }: AlertProps) {
  const { box, icon: Icon } = TONES[tone]
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={cn('flex gap-3 rounded-control border p-3.5 text-sm', box, className)}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden />
      <div className="min-w-0">
        <p className="font-semibold">{title}</p>
        {children && <div className="mt-0.5 text-text/90">{children}</div>}
      </div>
    </div>
  )
}
