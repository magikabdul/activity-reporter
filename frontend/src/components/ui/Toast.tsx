import { X } from 'lucide-react'
import { cn } from '@/lib/cn'
import { TONES, type Tone } from './tones'

export interface ToastProps {
  tone: Tone
  title: string
  description?: string
  /** ms until auto-dismiss; omit for a toast that stays until closed (errors) */
  duration?: number
  onClose: () => void
}

/** Floating feedback for the result of an action. Same tones/icons as <Alert>. */
export function Toast({ tone, title, description, duration, onClose }: ToastProps) {
  const { icon: Icon, text, solid } = TONES[tone]

  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className="relative flex w-[22rem] max-w-[calc(100vw-2rem)] gap-3 overflow-hidden rounded-control border border-border bg-surface-2 py-3 pr-3 pl-4 text-sm shadow-xl shadow-black/40"
    >
      <span className={cn('absolute inset-y-0 left-0 w-1', solid)} aria-hidden />
      <Icon className={cn('mt-0.5 size-4 shrink-0', text)} aria-hidden />
      <div className="min-w-0 flex-1">
        <p className="font-semibold text-text">{title}</p>
        {description && <p className="mt-0.5 break-words text-muted">{description}</p>}
      </div>
      <button
        type="button"
        onClick={onClose}
        aria-label="Dismiss notification"
        className="flex size-6 shrink-0 cursor-pointer items-center justify-center rounded-control text-muted transition hover:bg-surface hover:text-text"
      >
        <X className="size-3.5" aria-hidden />
      </button>
      {duration !== undefined && (
        <span
          aria-hidden
          className={cn('toast-timer absolute inset-x-0 bottom-0 h-0.5 origin-left', solid)}
          style={{ animationDuration: `${duration}ms` }}
        />
      )}
    </div>
  )
}
