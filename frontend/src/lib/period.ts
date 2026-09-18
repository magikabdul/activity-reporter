import type { ReportPeriod } from '@/api/types'

export const MONTH_NAMES = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
] as const

export function currentPeriod(now: Date = new Date()): ReportPeriod {
  return { year: now.getFullYear(), month: now.getMonth() + 1 }
}

export function shiftPeriod({ year, month }: ReportPeriod, delta: number): ReportPeriod {
  const index = year * 12 + (month - 1) + delta
  return { year: Math.floor(index / 12), month: (index % 12) + 1 }
}

export function isAfter(a: ReportPeriod, b: ReportPeriod): boolean {
  return a.year * 12 + a.month > b.year * 12 + b.month
}

/** "2026-09" */
export function periodKey({ year, month }: ReportPeriod): string {
  return `${year}-${String(month).padStart(2, '0')}`
}

/** "September 2026" */
export function periodLabel({ year, month }: ReportPeriod): string {
  return `${MONTH_NAMES[month - 1] ?? month} ${year}`
}
