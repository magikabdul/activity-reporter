import { describe, expect, it } from 'vitest'
import { toCsv } from './export'
import { currentPeriod, isAfter, periodKey, periodLabel, shiftPeriod } from './period'
import { loadSuggestions, rememberFromReport, rememberTask } from './suggestions'

describe('toCsv', () => {
  it('writes a BOM, semicolons and CRLF, quoting only when needed', () => {
    const csv = toCsv({
      headers: ['Customer', 'Description', 'Hours'],
      rows: [['ORLEN', 'Spotkanie; analiza "wymagań"\nnotatki', 3]],
    })

    expect(csv.charCodeAt(0)).toBe(0xfeff)
    expect(csv.slice(1)).toBe(
      'Customer;Description;Hours\r\nORLEN;"Spotkanie; analiza ""wymagań""\nnotatki";3\r\n',
    )
  })
})

describe('period', () => {
  it('shifts across year boundaries', () => {
    expect(shiftPeriod({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 })
    expect(shiftPeriod({ year: 2025, month: 12 }, 1)).toEqual({ year: 2026, month: 1 })
    expect(shiftPeriod({ year: 2026, month: 9 }, -12)).toEqual({ year: 2025, month: 9 })
  })

  it('formats keys and labels', () => {
    expect(periodKey({ year: 2026, month: 9 })).toBe('2026-09')
    expect(periodLabel({ year: 2026, month: 9 })).toBe('September 2026')
    expect(currentPeriod(new Date(2026, 8, 18))).toEqual({ year: 2026, month: 9 })
  })

  it('compares periods', () => {
    expect(isAfter({ year: 2026, month: 10 }, { year: 2026, month: 9 })).toBe(true)
    expect(isAfter({ year: 2026, month: 9 }, { year: 2026, month: 9 })).toBe(false)
  })
})

describe('suggestions', () => {
  it('keeps the most recently used value first without duplicates', () => {
    rememberTask('orlen', { firstName: 'Anna', lastName: 'Nowak' })
    rememberTask('PKO', { firstName: 'Jan', lastName: 'Kowalski' })
    rememberTask('Orlen', { firstName: 'Anna', lastName: 'Nowak' })

    expect(loadSuggestions()).toEqual({
      customers: ['ORLEN', 'PKO'],
      salesmen: [
        { firstName: 'Anna', lastName: 'Nowak' },
        { firstName: 'Jan', lastName: 'Kowalski' },
      ],
    })
  })

  it('learns customers and salesmen from a fetched report', () => {
    rememberFromReport([
      {
        createdAt: '2026-09-01',
        company: 'ORLEN',
        description: 'Work',
        salesman: 'Ewa Żak-Nowak',
        hoursSpent: 1,
      },
    ])

    expect(loadSuggestions()).toEqual({
      customers: ['ORLEN'],
      salesmen: [{ firstName: 'Ewa', lastName: 'Żak-Nowak' }],
    })
  })
})
