import type { TrecomReportItem } from '@/api/types'

export interface HoursBucket {
  label: string
  hours: number
  tasks: number
  /** 0–1, relative to the total */
  share: number
}

export interface TrecomSummary {
  totalHours: number
  totalTasks: number
  byCustomer: HoursBucket[]
  bySalesman: HoursBucket[]
}

function groupHours(
  items: TrecomReportItem[],
  keyOf: (item: TrecomReportItem) => string,
  totalHours: number,
): HoursBucket[] {
  const buckets = new Map<string, { hours: number; tasks: number }>()
  for (const item of items) {
    const label = keyOf(item)?.trim() || '—'
    const bucket = buckets.get(label) ?? { hours: 0, tasks: 0 }
    bucket.hours += item.hoursSpent
    bucket.tasks += 1
    buckets.set(label, bucket)
  }
  return [...buckets.entries()]
    .map(([label, { hours, tasks }]) => ({
      label,
      hours,
      tasks,
      share: totalHours > 0 ? hours / totalHours : 0,
    }))
    .sort((a, b) => b.hours - a.hours || a.label.localeCompare(b.label))
}

export function summarize(items: TrecomReportItem[]): TrecomSummary {
  const totalHours = items.reduce((sum, item) => sum + item.hoursSpent, 0)
  return {
    totalHours,
    totalTasks: items.length,
    byCustomer: groupHours(items, (item) => item.company, totalHours),
    bySalesman: groupHours(items, (item) => item.salesman, totalHours),
  }
}
