import { describe, expect, it } from 'vitest'
import { loadCachedReport, markReportStale, saveCachedReport } from './reportCache'

const september = { year: 2026, month: 9 }
const items = [{ name: 'DOCUMENTATION', description: 'Opis kategorii', summary: 'Podsumowanie' }]

describe('reportCache', () => {
  it('stores a generated report per month', () => {
    saveCachedReport(september, items)

    expect(loadCachedReport(september)?.items).toEqual(items)
    expect(loadCachedReport(september)?.stale).toBeUndefined()
    expect(loadCachedReport({ year: 2026, month: 8 })).toBeNull()
  })

  it('flags the cached report as stale and clears the flag when regenerated', () => {
    saveCachedReport(september, items)

    markReportStale(september)
    expect(loadCachedReport(september)?.stale).toBe(true)
    expect(loadCachedReport(september)?.items).toEqual(items)

    saveCachedReport(september, items)
    expect(loadCachedReport(september)?.stale).toBeUndefined()
  })

  it('does nothing when no report was generated for that month', () => {
    markReportStale(september)

    expect(loadCachedReport(september)).toBeNull()
  })
})
