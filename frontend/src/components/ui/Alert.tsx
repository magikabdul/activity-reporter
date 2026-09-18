import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { TONES, type Tone } from './tones'

interface AlertProps {
  tone?: Tone
  title: string
  children?: ReactNode
  /** e.g. a button that resolves the situation */
  action?: ReactNode
  className?: string
}

/** Inline feedback, shown where the problem is. For the result of an action use `notify` (toast). */
export function Alert({ tone = 'info', title, children, action, className }: AlertProps) {
  const { box, text, icon: Icon } = TONES[tone]
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={cn('flex gap-3 rounded-control border p-3.5 text-sm', box, text, className)}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{title}</p>
        {children && <div className="mt-0.5 text-text/90">{children}</div>}
      </div>
      {action && <div className="no-print shrink-0 self-center">{action}</div>}
    </div>
  )
}
