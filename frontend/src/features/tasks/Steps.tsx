import { Check } from 'lucide-react'
import { cn } from '@/lib/cn'
import type { TaskFlowStep } from './useTaskFlow'

const STEPS: { id: TaskFlowStep; label: string }[] = [
  { id: 'form', label: 'Describe' },
  { id: 'review', label: 'Review AI result' },
  { id: 'saved', label: 'Saved' },
]

export function Steps({ current }: { current: TaskFlowStep }) {
  const currentIndex = STEPS.findIndex((step) => step.id === current)

  return (
    <ol className="mb-6 flex flex-wrap items-center gap-x-3 gap-y-2 text-sm">
      {STEPS.map((step, index) => {
        const done = index < currentIndex || current === 'saved'
        const active = index === currentIndex && !done
        return (
          <li key={step.id} className="flex items-center gap-2.5">
            {index > 0 && <span className="h-px w-6 bg-border" aria-hidden />}
            <span
              aria-current={active ? 'step' : undefined}
              className={cn(
                'flex size-6 items-center justify-center rounded-full border text-xs font-semibold',
                done && 'border-accent bg-accent text-accent-fg',
                active && 'border-accent text-accent',
                !done && !active && 'border-border text-muted',
              )}
            >
              {done ? <Check className="size-3.5" aria-hidden /> : index + 1}
            </span>
            <span className={cn('font-medium', done || active ? 'text-text' : 'text-muted')}>
              {step.label}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
