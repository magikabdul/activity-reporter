import { useQuery } from '@tanstack/react-query'
import { FilePlus2, FileText, ListChecks } from 'lucide-react'
import { lufthansaApi } from '@/api/lufthansa'
import { trecomApi } from '@/api/trecom'
import { PageHeader } from '@/components/layout/PageHeader'
import { ButtonLink } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Skeleton } from '@/components/ui/Skeleton'
import { tasksQueryKey } from '@/features/tasks/useTaskChangeSync'
import { formatIsoDate } from '@/lib/date'
import { currentPeriod, periodKey, periodLabel } from '@/lib/period'
import { COMPANY_LIST, type CompanyId } from '@/theme/companies'

interface MonthStat {
  label: string
  value: string
}

/** Same query keys as the Tasks pages, so the numbers come from (and warm) the same cache. */
function useMonthStats(company: CompanyId) {
  const period = currentPeriod()

  return useQuery({
    queryKey: tasksQueryKey(company, periodKey(period)),
    queryFn: (): Promise<{ createdAt: string; hoursSpent?: number }[]> =>
      company === 'trecom' ? trecomApi.getTasks(period) : lufthansaApi.getTasks(period),
    staleTime: 30_000,
    select: (tasks): MonthStat[] => {
      const lastEntry = tasks
        .map((task) => task.createdAt)
        .sort()
        .at(-1)
      return [
        { label: 'Tasks', value: String(tasks.length) },
        ...(company === 'trecom'
          ? [
              {
                label: 'Hours',
                value: `${tasks.reduce((sum, task) => sum + (task.hoursSpent ?? 0), 0)} h`,
              },
            ]
          : []),
        { label: 'Last entry', value: lastEntry ? formatIsoDate(lastEntry) : '—' },
      ]
    },
  })
}

function MonthStats({ company }: { company: CompanyId }) {
  const stats = useMonthStats(company)

  if (stats.isPending) return <Skeleton className="h-14" />
  if (stats.isError) {
    return <p className="text-sm text-muted">This month&apos;s numbers are unavailable.</p>
  }

  return (
    <dl className="flex flex-wrap gap-x-8 gap-y-3 border-y border-border py-3.5">
      {stats.data.map((stat) => (
        <div key={stat.label}>
          <dt className="text-xs font-semibold tracking-wide text-muted uppercase">{stat.label}</dt>
          <dd className="mt-0.5 text-lg font-semibold text-text tabular-nums">{stat.value}</dd>
        </div>
      ))}
    </dl>
  )
}

export function HomePage() {
  return (
    <>
      <PageHeader
        title="Reporter"
        description={`Log your work and generate monthly reports. Numbers below are for ${periodLabel(currentPeriod())}.`}
      />

      <div className="grid gap-5 md:grid-cols-2">
        {COMPANY_LIST.map((company) => (
          // data-company scopes the company's palette to its card
          <Card
            key={company.id}
            data-company={company.id}
            className="relative flex flex-col gap-5 overflow-hidden bg-surface"
          >
            <span className="absolute inset-x-0 top-0 h-1 bg-accent" aria-hidden />
            <div>
              <p className="text-xs font-semibold tracking-[0.2em] text-accent uppercase">Client</p>
              <h2 className="mt-1.5 text-xl font-semibold tracking-tight text-text">
                {company.name}
              </h2>
              <p className="mt-1.5 text-sm text-muted">{company.tagline}</p>
            </div>
            <MonthStats company={company.id} />
            <div className="mt-auto flex flex-wrap gap-3">
              <ButtonLink
                to={`${company.basePath}/new-task`}
                variant="primary"
                icon={<FilePlus2 className="size-4" aria-hidden />}
              >
                New task
              </ButtonLink>
              <ButtonLink
                to={`${company.basePath}/tasks`}
                icon={<ListChecks className="size-4" aria-hidden />}
              >
                Tasks
              </ButtonLink>
              <ButtonLink
                to={`${company.basePath}/report`}
                icon={<FileText className="size-4" aria-hidden />}
              >
                Report
              </ButtonLink>
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}
