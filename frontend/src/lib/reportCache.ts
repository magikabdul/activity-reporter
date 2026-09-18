import type { LufthansaReportItem, ReportPeriod } from '@/api/types'
import { periodKey } from './period'
import { readJson, writeJson } from './storage'

/**
 * The Lufthansa report is AI-generated (one OpenAI call per category on every GET), so its last
 * result is kept per month in localStorage. When a task of that month is added, edited or deleted
 * afterwards, the cached report is flagged as stale instead of being silently wrong.
 */
export interface CachedReport {
  items: LufthansaReportItem[]
  generatedAt: number
  stale?: boolean
}

const key = (period: ReportPeriod) => `reporter.lufthansa.report.${periodKey(period)}`

export function loadCachedReport(period: ReportPeriod): CachedReport | null {
  return readJson<CachedReport | null>(localStorage, key(period), null)
}

export function saveCachedReport(period: ReportPeriod, items: LufthansaReportItem[]): void {
  writeJson(localStorage, key(period), { items, generatedAt: Date.now() } satisfies CachedReport)
}

export function markReportStale(period: ReportPeriod): void {
  const cached = loadCachedReport(period)
  if (cached && !cached.stale) writeJson(localStorage, key(period), { ...cached, stale: true })
}
