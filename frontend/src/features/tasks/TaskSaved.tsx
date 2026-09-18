import { CircleCheck, FileText, Plus } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'

interface TaskSavedProps {
  summary: ReactNode
  reportPath: string
  onStartNew: () => void
}

export function TaskSaved({ summary, reportPath, onStartNew }: TaskSavedProps) {
  return (
    <Card className="flex flex-col items-start gap-4">
      <span className="flex size-11 items-center justify-center rounded-full bg-success-soft text-success">
        <CircleCheck className="size-5" aria-hidden />
      </span>
      <div>
        <h2 className="text-base font-semibold text-text">Task saved</h2>
        <div className="mt-1.5 text-sm leading-relaxed text-muted">{summary}</div>
      </div>
      <div className="flex flex-wrap gap-3">
        <Button
          variant="primary"
          icon={<Plus className="size-4" aria-hidden />}
          onClick={onStartNew}
        >
          Add another task
        </Button>
        <ButtonLink to={reportPath} icon={<FileText className="size-4" aria-hidden />}>
          Open report
        </ButtonLink>
      </div>
    </Card>
  )
}
