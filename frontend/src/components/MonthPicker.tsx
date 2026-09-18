import { ChevronLeft, ChevronRight } from 'lucide-react'
import type { ReportPeriod } from '@/api/types'
import { currentPeriod, isAfter, periodLabel, shiftPeriod } from '@/lib/period'

interface MonthPickerProps {
  value: ReportPeriod
  onChange: (period: ReportPeriod) => void
  disabled?: boolean
}

const STEP =
  'flex size-10 cursor-pointer items-center justify-center text-muted transition hover:bg-surface-2 hover:text-text ' +
  'disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent'

export function MonthPicker({ value, onChange, disabled }: MonthPickerProps) {
  const next = shiftPeriod(value, 1)
  const nextIsFuture = isAfter(next, currentPeriod())

  return (
    <div
      role="group"
      aria-label="Report month"
      className="inline-flex h-10 items-center overflow-hidden rounded-control border border-border bg-surface"
    >
      <button
        type="button"
        aria-label="Previous month"
        disabled={disabled}
        onClick={() => onChange(shiftPeriod(value, -1))}
        className={STEP}
      >
        <ChevronLeft className="size-4" aria-hidden />
      </button>
      <span
        aria-live="polite"
        className="min-w-40 border-x border-border px-4 text-center text-sm font-medium text-text tabular-nums"
      >
        {periodLabel(value)}
      </span>
      <button
        type="button"
        aria-label="Next month"
        disabled={disabled || nextIsFuture}
        onClick={() => onChange(next)}
        className={STEP}
      >
        <ChevronRight className="size-4" aria-hidden />
      </button>
    </div>
  )
}
