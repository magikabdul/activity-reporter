import { ArrowRight, FilePlus2, FileText } from 'lucide-react'
import { PageHeader } from '@/components/layout/PageHeader'
import { ButtonLink } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { currentPeriod, periodLabel } from '@/lib/period'
import { COMPANY_LIST } from '@/theme/companies'

export function HomePage() {
  return (
    <>
      <PageHeader
        title="Reporter"
        description={`Log your work and generate monthly reports. Current period: ${periodLabel(currentPeriod())}.`}
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
            <div className="mt-auto flex flex-wrap gap-3">
              <ButtonLink
                to={`${company.basePath}/new-task`}
                variant="primary"
                icon={<FilePlus2 className="size-4" aria-hidden />}
              >
                New task
              </ButtonLink>
              <ButtonLink
                to={`${company.basePath}/report`}
                icon={<FileText className="size-4" aria-hidden />}
              >
                Report
                <ArrowRight className="size-3.5 text-muted" aria-hidden />
              </ButtonLink>
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}
