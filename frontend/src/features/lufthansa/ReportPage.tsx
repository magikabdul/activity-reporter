import { useQuery } from '@tanstack/react-query'
import { Copy, FileSearch, RefreshCw, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { ApiError, errorMessage } from '@/api/client'
import { lufthansaApi } from '@/api/lufthansa'
import type { LufthansaReportItem, ReportPeriod } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { MonthPicker } from '@/components/MonthPicker'
import { ReportToolbar } from '@/components/ReportToolbar'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Card, CardHeader } from '@/components/ui/Card'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { copyText } from '@/lib/clipboard'
import type { Sheet } from '@/lib/export'
import { currentPeriod, periodKey, periodLabel } from '@/lib/period'
import { readJson, writeJson } from '@/lib/storage'
import { COMPANIES } from '@/theme/companies'

const company = COMPANIES.lufthansa

interface CachedReport {
  items: LufthansaReportItem[]
  generatedAt: number
}

const cacheKey = (period: ReportPeriod) => `reporter.lufthansa.report.${periodKey(period)}`

/**
 * Every GET makes the backend call OpenAI once per category, so the report is only fetched on an
 * explicit click and the result is kept in localStorage per month.
 */
function useLufthansaReport(period: ReportPeriod) {
  const cached = readJson<CachedReport | null>(localStorage, cacheKey(period), null)

  return useQuery({
    queryKey: ['lufthansa-report', periodKey(period)],
    enabled: false,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
    initialData: cached?.items,
    initialDataUpdatedAt: cached?.generatedAt,
    queryFn: async () => {
      try {
        const items = await lufthansaApi.getReport(period)
        writeJson(localStorage, cacheKey(period), { items, generatedAt: Date.now() })
        return items
      } catch (error) {
        if (error instanceof ApiError && error.isEmptyResult) return []
        throw error
      }
    },
  })
}

function toSheet(items: LufthansaReportItem[]): Sheet {
  return {
    name: 'Lufthansa',
    headers: ['Category', 'Category description', 'Summary'],
    rows: items.map((item) => [item.name, item.description, item.summary]),
  }
}

function toText(items: LufthansaReportItem[], period: ReportPeriod): string {
  const body = items.map((item) => `${item.name}\n${item.summary}`).join('\n\n')
  return `${company.name} — ${periodLabel(period)}\n\n${body}\n`
}

export function LufthansaReportPage() {
  const [period, setPeriod] = useState(currentPeriod)
  const report = useLufthansaReport(period)
  const items = report.data

  async function copySummary(item: LufthansaReportItem) {
    if (await copyText(item.summary)) toast.success(`Copied: ${item.name}`)
    else toast.error('Could not access the clipboard')
  }

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="Monthly report"
        description="One AI-written summary per work category, based on the tasks saved in the selected month."
        actions={<MonthPicker value={period} onChange={setPeriod} disabled={report.isFetching} />}
      />
      <p className="mb-4 hidden text-sm print:block">{periodLabel(period)}</p>

      <div className="no-print mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant={items ? 'secondary' : 'primary'}
            loading={report.isFetching}
            icon={
              items ? (
                <RefreshCw className="size-4" aria-hidden />
              ) : (
                <Sparkles className="size-4" aria-hidden />
              )
            }
            onClick={() => report.refetch()}
          >
            {report.isFetching ? 'Generating…' : items ? 'Regenerate' : 'Generate report'}
          </Button>
          {items && items.length > 0 && !report.isFetching && (
            <span className="text-xs text-muted">
              Generated {new Date(report.dataUpdatedAt).toLocaleString('en-GB')} · cached in this
              browser
            </span>
          )}
        </div>
        {items && items.length > 0 && (
          <ReportToolbar
            fileName={`lufthansa-report-${periodKey(period)}`}
            sheet={toSheet(items)}
            text={toText(items, period)}
          />
        )}
      </div>

      {report.isError && !report.isFetching && (
        <Alert tone="danger" title="Report generation failed" className="mb-6">
          {errorMessage(report.error)}
        </Alert>
      )}

      {report.isFetching ? (
        <div className="flex flex-col gap-4" aria-busy="true">
          <p className="text-sm text-muted">
            AI is summarising every category — this can take up to a minute.
          </p>
          {[0, 1, 2].map((index) => (
            <Card key={index}>
              <Skeleton className="mb-4 h-5 w-56" />
              <Skeleton className="mb-2 h-3.5 w-full" />
              <Skeleton className="h-3.5 w-3/4" />
            </Card>
          ))}
        </div>
      ) : items === undefined ? (
        !report.isError && (
          <EmptyState
            icon={Sparkles}
            title={`No report generated for ${periodLabel(period)} yet`}
            description="Generating calls OpenAI once per category, so it only runs when you ask for it. The result is then cached in this browser."
          />
        )
      ) : items.length === 0 ? (
        <EmptyState
          icon={FileSearch}
          title={`No tasks in ${periodLabel(period)}`}
          description="There is nothing to summarise for this month."
        />
      ) : (
        <div className="flex flex-col gap-4">
          {items.map((item) => (
            <Card key={item.name} className="print-avoid-break">
              <CardHeader
                title={item.name}
                description={item.description}
                actions={
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<Copy className="size-3.5" aria-hidden />}
                    onClick={() => copySummary(item)}
                  >
                    Copy
                  </Button>
                }
              />
              <p className="text-sm leading-relaxed whitespace-pre-line text-text">
                {item.summary}
              </p>
            </Card>
          ))}
        </div>
      )}
    </>
  )
}
