import { ArrowRight, Check, Pencil, Sparkles } from 'lucide-react'
import type { ReactNode } from 'react'
import { errorMessage } from '@/api/client'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card, CardHeader } from '@/components/ui/Card'
import { ShortcutHint } from '@/components/ui/Kbd'
import { useSubmitShortcut } from '@/lib/useShortcut'

export interface ReviewRow {
  label: string
  /** what the user typed; omit for values the AI produced on its own (e.g. a category) */
  input?: string
  result: ReactNode
  /** highlights rows the AI modified */
  changed?: boolean
}

interface TaskReviewProps {
  rows: ReviewRow[]
  /** shown above the rows - e.g. that AI could not assign a category and one has to be picked */
  notice?: ReactNode
  footnote?: ReactNode
  error?: unknown
  isCompleting: boolean
  /** blocks the primary action while something in the review is still unresolved */
  confirmDisabled?: boolean
  onConfirm: () => void
  onEdit: () => void
}

export function TaskReview({
  rows,
  notice,
  footnote,
  error,
  isCompleting,
  confirmDisabled,
  onConfirm,
  onEdit,
}: TaskReviewProps) {
  useSubmitShortcut(onConfirm, !isCompleting && !confirmDisabled)

  return (
    <Card>
      <CardHeader
        title="AI result"
        description="Nothing is stored yet. Check what will be saved, then confirm."
        actions={
          <Badge>
            <Sparkles className="size-3" aria-hidden />
            AI
          </Badge>
        }
      />

      {notice && <div className="mb-5">{notice}</div>}

      <dl className="flex flex-col divide-y divide-border/70">
        {rows.map((row) => (
          <div key={row.label} className="grid gap-2 py-4 first:pt-0 sm:grid-cols-[9rem_1fr]">
            <dt className="flex flex-row items-center gap-2 text-sm font-medium text-muted sm:flex-col sm:items-start sm:gap-1.5">
              {row.label}
              {row.changed && <Badge>corrected</Badge>}
            </dt>
            <dd className="min-w-0 text-sm leading-relaxed text-text">
              {row.changed && row.input !== undefined && (
                <p className="mb-1.5 flex items-start gap-2 text-muted line-through decoration-muted/50">
                  {row.input}
                </p>
              )}
              <div className="flex items-start gap-2">
                {row.changed && (
                  <ArrowRight className="mt-1 size-3.5 shrink-0 text-accent" aria-hidden />
                )}
                <div className="min-w-0 break-words">{row.result}</div>
              </div>
            </dd>
          </div>
        ))}
      </dl>

      {footnote && <p className="mt-2 text-xs text-muted">{footnote}</p>}

      {error != null && (
        <Alert tone="danger" title="Saving failed" className="mt-5">
          {errorMessage(error)}
        </Alert>
      )}

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <Button
          variant="primary"
          loading={isCompleting}
          disabled={confirmDisabled}
          icon={<Check className="size-4" aria-hidden />}
          onClick={onConfirm}
        >
          Confirm &amp; save
        </Button>
        <Button
          disabled={isCompleting}
          icon={<Pencil className="size-4" aria-hidden />}
          onClick={onEdit}
        >
          Edit again
        </Button>
        <ShortcutHint keys={['Ctrl', 'Enter']} label="confirm" />
      </div>
    </Card>
  )
}
