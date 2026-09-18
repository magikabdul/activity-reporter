import type { ReportPeriod } from '@/api/types'

/** Local calendar day as yyyy-mm-dd (what <input type="date"> and the backend's LocalDate use). */
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function periodOfIso(isoDate: string): ReportPeriod {
  const [year, month] = isoDate.split('-').map(Number)
  return { year: year ?? 0, month: month ?? 0 }
}

/** "2026-09-11" → "11 Sep 2026" */
export function formatIsoDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  if (!year || !month || !day) return isoDate
  return new Date(year, month - 1, day).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}
