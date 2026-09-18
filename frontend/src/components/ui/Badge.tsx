import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

type Tone = 'accent' | 'muted' | 'success'

const TONES: Record<Tone, string> = {
  accent: 'bg-accent-soft text-accent border-accent/30',
  muted: 'bg-surface-2 text-muted border-border',
  success: 'bg-success-soft text-success border-success/30',
}

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: Tone
}

export function Badge({ tone = 'accent', className, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold tracking-wide',
        TONES[tone],
        className,
      )}
      {...props}
    />
  )
}
