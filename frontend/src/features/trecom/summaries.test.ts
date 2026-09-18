import { describe, expect, it } from 'vitest'
import type { TrecomReportItem } from '@/api/types'
import { summarize } from './summaries'

const item = (company: string, salesman: string, hoursSpent: number): TrecomReportItem => ({
  createdAt: '2026-09-01',
  company,
  description: 'Some work done',
  salesman,
  hoursSpent,
})

describe('summarize', () => {
  it('returns zeros for an empty report', () => {
    expect(summarize([])).toEqual({
      totalHours: 0,
      totalTasks: 0,
      byCustomer: [],
      bySalesman: [],
    })
  })

  it('totals hours and groups them per customer and salesman, largest first', () => {
    const summary = summarize([
      item('ORLEN', 'Anna Nowak', 2),
      item('PKO', 'Jan Kowalski', 6),
      item('ORLEN', 'Jan Kowalski', 2),
    ])

    expect(summary.totalHours).toBe(10)
    expect(summary.totalTasks).toBe(3)
    expect(summary.byCustomer).toEqual([
      { label: 'PKO', hours: 6, tasks: 1, share: 0.6 },
      { label: 'ORLEN', hours: 4, tasks: 2, share: 0.4 },
    ])
    expect(summary.bySalesman).toEqual([
      { label: 'Jan Kowalski', hours: 8, tasks: 2, share: 0.8 },
      { label: 'Anna Nowak', hours: 2, tasks: 1, share: 0.2 },
    ])
  })
})
