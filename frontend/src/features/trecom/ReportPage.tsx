import { useQuery } from '@tanstack/react-query'
import { FileSearch, ListChecks, RefreshCw } from 'lucide-react'
import { useMemo, useState } from 'react'
import { ApiError, errorMessage } from '@/api/client'
import { trecomApi } from '@/api/trecom'
import type { ReportPeriod, TrecomReportItem } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { MonthPicker } from '@/components/MonthPicker'
import { ReportToolbar } from '@/components/ReportToolbar'
import { Alert } from '@/components/ui/Alert'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Card, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { Table, Td, Th, type SortDirection } from '@/components/ui/Table'
import type { Sheet } from '@/lib/export'
import { currentPeriod, periodKey, periodLabel } from '@/lib/period'
import { rememberFromReport } from '@/lib/suggestions'
import { COMPANIES } from '@/theme/companies'
import { summarize, type HoursBucket } from './summaries'

const company = COMPANIES.trecom

type SortKey = keyof TrecomReportItem

const COLUMNS: { key: SortKey; label: string; align?: 'right' }[] = [
  { key: 'createdAt', label: 'Date' },
  { key: 'company', label: 'Customer' },
  { key: 'description', label: 'Description' },
  { key: 'salesman', label: 'Salesman' },
  { key: 'hoursSpent', label: 'Hours', align: 'right' },
]

function useTrecomReport(period: ReportPeriod) {
  return useQuery({
    queryKey: ['trecom-report', periodKey(period)],
    staleTime: 60_000,
    retry: false,
    queryFn: async () => {
      try {
        const items = await trecomApi.getReport(period)
        rememberFromReport(items)
        return items
      } catch (error) {
        // the backend answers an empty month with a body-less 404
        if (error instanceof ApiError && error.isEmptyResult) return []
        throw error
      }
    },
  })
}

function toSheet(items: TrecomReportItem[], totalHours: number): Sheet {
  return {
    name: 'Trecom',
    headers: ['Date', 'Customer', 'Description', 'Salesman', 'Hours'],
    rows: [
      ...items.map((item) => [
        item.createdAt,
        item.company,
        item.description,
        item.salesman,
        item.hoursSpent,
      ]),
      ['', '', '', 'Total', totalHours],
    ],
  }
}

/** Tab-separated, so it pastes straight into a spreadsheet or an e-mail. */
function toText(sheet: Sheet, period: ReportPeriod): string {
  const lines = [sheet.headers, ...sheet.rows].map((row) => row.join('\t'))
  return `${company.name} — ${periodLabel(period)}\n\n${lines.join('\n')}\n`
}

export function TrecomReportPage() {
  const [period, setPeriod] = useState(currentPeriod)
  const [sort, setSort] = useState<{ key: SortKey; direction: SortDirection }>({
    key: 'createdAt',
    direction: 'asc',
  })
  const report = useTrecomReport(period)

  const items = report.data
  const summary = useMemo(() => summarize(items ?? []), [items])
  const sorted = useMemo(() => {
    const factor = sort.direction === 'asc' ? 1 : -1
    return [...(items ?? [])].sort((a, b) => {
      const left = a[sort.key]
      const right = b[sort.key]
      const result =
        typeof left === 'number' && typeof right === 'number'
          ? left - right
          : String(left ?? '').localeCompare(String(right ?? ''), 'pl')
      return result * factor
    })
  }, [items, sort])

  function toggleSort(key: SortKey) {
    setSort((current) =>
      current.key === key
        ? { key, direction: current.direction === 'asc' ? 'desc' : 'asc' }
        : { key, direction: 'asc' },
    )
  }

  const sheet = toSheet(sorted, summary.totalHours)

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="Monthly report"
        description="Every task saved in the selected month, with hours per customer and per salesman."
        actions={<MonthPicker value={period} onChange={setPeriod} />}
      />
      <p className="mb-4 hidden text-sm print:block">{periodLabel(period)}</p>

      <div className="no-print mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <Button
            loading={report.isFetching}
            icon={<RefreshCw className="size-4" aria-hidden />}
            onClick={() => report.refetch()}
          >
            Refresh
          </Button>
          <ButtonLink
            to={`${company.basePath}/tasks`}
            icon={<ListChecks className="size-4" aria-hidden />}
          >
            Review tasks
          </ButtonLink>
        </div>
        {items && items.length > 0 && (
          <ReportToolbar
            fileName={`trecom-report-${periodKey(period)}`}
            sheet={sheet}
            text={toText(sheet, period)}
          />
        )}
      </div>

      {report.isError ? (
        <Alert tone="danger" title="Could not load the report">
          {errorMessage(report.error)}
        </Alert>
      ) : report.isPending ? (
        <div className="flex flex-col gap-4" aria-busy="true">
          <div className="grid gap-4 sm:grid-cols-3">
            {[0, 1, 2].map((index) => (
              <Skeleton key={index} className="h-24" />
            ))}
          </div>
          <Skeleton className="h-64" />
        </div>
      ) : sorted.length === 0 ? (
        <EmptyState
          icon={FileSearch}
          title={`No tasks in ${periodLabel(period)}`}
          description="Nothing was logged for Trecom in this month."
        />
      ) : (
        <div className="flex flex-col gap-6">
          <div className="grid gap-4 sm:grid-cols-3">
            <Stat label="Total hours" value={`${summary.totalHours} h`} />
            <Stat label="Tasks" value={String(summary.totalTasks)} />
            <Stat label="Customers" value={String(summary.byCustomer.length)} />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Breakdown title="Hours per customer" buckets={summary.byCustomer} />
            <Breakdown title="Hours per salesman" buckets={summary.bySalesman} />
          </div>

          <Table>
            <thead>
              <tr>
                {COLUMNS.map((column) => (
                  <Th
                    key={column.key}
                    align={column.align}
                    sorted={sort.key === column.key ? sort.direction : null}
                    onSort={() => toggleSort(column.key)}
                  >
                    {column.label}
                  </Th>
                ))}
              </tr>
            </thead>
            <tbody>
              {sorted.map((item, index) => (
                <tr key={`${item.createdAt}-${index}`} className="print-avoid-break">
                  <Td className="whitespace-nowrap tabular-nums">{item.createdAt}</Td>
                  <Td className="font-medium">{item.company}</Td>
                  <Td className="min-w-64 text-text/90">{item.description}</Td>
                  <Td className="whitespace-nowrap">{item.salesman}</Td>
                  <Td align="right">{item.hoursSpent}</Td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <Td colSpan={4} className="bg-surface-2 text-right font-semibold">
                  Total
                </Td>
                <Td align="right" className="bg-surface-2 font-semibold text-accent">
                  {summary.totalHours}
                </Td>
              </tr>
            </tfoot>
          </Table>
        </div>
      )}
    </>
  )
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Card className="print-avoid-break">
      <p className="text-xs font-semibold tracking-wide text-muted uppercase">{label}</p>
      <p className="mt-2 text-3xl font-semibold tracking-tight text-text tabular-nums">{value}</p>
    </Card>
  )
}

function Breakdown({ title, buckets }: { title: string; buckets: HoursBucket[] }) {
  return (
    <Card className="print-avoid-break">
      <CardHeader title={title} />
      <ul className="flex flex-col gap-3.5">
        {buckets.map((bucket) => (
          <li key={bucket.label}>
            <div className="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
              <span className="min-w-0 truncate font-medium text-text">{bucket.label}</span>
              <span className="shrink-0 text-muted tabular-nums">
                <span className="font-semibold text-text">{bucket.hours} h</span> ·{' '}
                {Math.round(bucket.share * 100)}% · {bucket.tasks}{' '}
                {bucket.tasks === 1 ? 'task' : 'tasks'}
              </span>
            </div>
            <div className="h-1.5 overflow-hidden rounded-full bg-surface-2">
              <div
                className="h-full rounded-full bg-accent"
                style={{ width: `${Math.max(bucket.share * 100, 2)}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </Card>
  )
}
