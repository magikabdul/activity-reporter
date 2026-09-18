import type { Salesman, TrecomReportItem } from '@/api/types'
import { readJson, writeJson } from './storage'

// The backend has no dictionary endpoint, so previously used values are remembered per browser.
const KEY = 'reporter.trecom.suggestions'
const LIMIT = 50

export interface TrecomSuggestions {
  customers: string[]
  salesmen: Salesman[]
}

const EMPTY: TrecomSuggestions = { customers: [], salesmen: [] }

export function loadSuggestions(): TrecomSuggestions {
  const stored = readJson<Partial<TrecomSuggestions>>(localStorage, KEY, EMPTY)
  return { customers: stored.customers ?? [], salesmen: stored.salesmen ?? [] }
}

function sameSalesman(a: Salesman, b: Salesman): boolean {
  return a.firstName === b.firstName && a.lastName === b.lastName
}

/** Most recently used first, de-duplicated, capped. */
export function rememberTask(customer: string, salesman: Salesman): TrecomSuggestions {
  const current = loadSuggestions()
  const name = customer.trim().toUpperCase()
  const next: TrecomSuggestions = {
    customers: [name, ...current.customers.filter((c) => c !== name)].slice(0, LIMIT),
    salesmen: [salesman, ...current.salesmen.filter((s) => !sameSalesman(s, salesman))].slice(
      0,
      LIMIT,
    ),
  }
  writeJson(localStorage, KEY, next)
  return next
}

/** Report rows carry the salesman as "FirstName LastName". */
export function rememberFromReport(items: TrecomReportItem[]): void {
  const current = loadSuggestions()
  const customers = [...current.customers]
  const salesmen = [...current.salesmen]

  for (const item of items) {
    const name = item.company?.trim().toUpperCase()
    if (name && !customers.includes(name)) customers.push(name)

    const [firstName, ...rest] = (item.salesman ?? '').trim().split(/\s+/)
    const lastName = rest.join(' ')
    if (firstName && lastName) {
      const salesman = { firstName, lastName }
      if (!salesmen.some((s) => sameSalesman(s, salesman))) salesmen.push(salesman)
    }
  }

  writeJson(localStorage, KEY, {
    customers: customers.slice(0, LIMIT),
    salesmen: salesmen.slice(0, LIMIT),
  })
}
