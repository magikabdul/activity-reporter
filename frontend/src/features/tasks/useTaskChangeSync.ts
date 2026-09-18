import { useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import { periodOfIso } from '@/lib/date'
import { periodKey } from '@/lib/period'
import { markReportStale } from '@/lib/reportCache'
import type { CompanyId } from '@/theme/companies'

export const tasksQueryKey = (company: CompanyId, key: string) => [`${company}-tasks`, key]

/**
 * Call after a task was added, edited or deleted, with every date involved (for an edit that moved
 * a task to another month: the old and the new date). Refreshes the task lists (Tasks page, Home),
 * the Trecom report, and flags the cached AI report of Lufthansa as out of date.
 */
export function useTaskChangeSync(company: CompanyId) {
  const queryClient = useQueryClient()

  return useCallback(
    (...isoDates: string[]) => {
      const periods = new Map(isoDates.map((date) => [periodKey(periodOfIso(date)), date]))

      for (const [key, date] of periods) {
        void queryClient.invalidateQueries({ queryKey: tasksQueryKey(company, key) })
        if (company === 'trecom') {
          void queryClient.invalidateQueries({ queryKey: ['trecom-report', key] })
        } else {
          markReportStale(periodOfIso(date))
        }
      }
    },
    [company, queryClient],
  )
}
