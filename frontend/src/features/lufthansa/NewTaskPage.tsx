import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { lufthansaApi } from '@/api/lufthansa'
import type { LufthansaCreateTaskRequest, LufthansaTaskResponse } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { TextAreaField } from '@/components/ui/Field'
import { Steps } from '@/features/tasks/Steps'
import { RegisterErrorAlert, StaleTaskAlert } from '@/features/tasks/TaskAlerts'
import { TaskReview } from '@/features/tasks/TaskReview'
import { TaskSaved } from '@/features/tasks/TaskSaved'
import { useTaskFlow } from '@/features/tasks/useTaskFlow'
import { DESCRIPTION_MAX } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { CATEGORY_DESCRIPTIONS, categoryLabel } from './categories'
import { lufthansaTaskSchema, type LufthansaTaskForm } from './schema'

const company = COMPANIES.lufthansa

export function LufthansaNewTaskPage() {
  const flow = useTaskFlow<LufthansaCreateTaskRequest, LufthansaTaskResponse>({
    company: company.id,
    register: lufthansaApi.registerTask,
    complete: lufthansaApi.completeTask,
    onSaved: () => toast.success('Lufthansa task saved'),
  })

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="New task"
        description="Describe what you did. AI corrects the wording and assigns the contractual work category before anything is stored."
      />
      <Steps current={flow.step} />

      {flow.step === 'form' && (
        <div className="flex flex-col gap-4">
          {flow.staleTask && <StaleTaskAlert />}
          <TaskForm
            key={flow.draft?.description ?? 'empty'}
            defaultValues={flow.draft ?? { description: '' }}
            isRegistering={flow.isRegistering}
            error={flow.registerError}
            onSubmit={flow.submit}
          />
        </div>
      )}

      {flow.step === 'review' && flow.pending && (
        <TaskReview
          rows={[
            {
              label: 'Category',
              result: (
                <div className="flex flex-col items-start gap-2">
                  <Badge>{categoryLabel(flow.pending.result.category)}</Badge>
                  <span className="text-muted">
                    {CATEGORY_DESCRIPTIONS[flow.pending.result.category]}
                  </span>
                </div>
              ),
            },
            {
              label: 'Description',
              input: flow.pending.input.description,
              result: flow.pending.result.description,
              changed: flow.pending.input.description !== flow.pending.result.description,
            },
          ]}
          error={flow.completeError}
          isCompleting={flow.isCompleting}
          onConfirm={flow.confirm}
          onEdit={flow.editAgain}
        />
      )}

      {flow.step === 'saved' && flow.saved && (
        <TaskSaved
          reportPath={`${company.basePath}/report`}
          onStartNew={flow.startNew}
          summary={
            <>
              <Badge className="mb-2">{categoryLabel(flow.saved.result.category)}</Badge>
              <p>{flow.saved.result.description}</p>
            </>
          }
        />
      )}
    </>
  )
}

interface TaskFormProps {
  defaultValues: LufthansaTaskForm
  isRegistering: boolean
  error: unknown
  onSubmit: (values: LufthansaTaskForm) => void
}

function TaskForm({ defaultValues, isRegistering, error, onSubmit }: TaskFormProps) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<LufthansaTaskForm>({
    resolver: zodResolver(lufthansaTaskSchema),
    defaultValues,
  })
  const length = useWatch({ control, name: 'description' }).trim().length

  return (
    <Card>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        <TextAreaField
          label="Description"
          placeholder="e.g. Analiza wymagań i implementacja endpointu do eksportu raportów…"
          hint={`${length} / ${DESCRIPTION_MAX}`}
          error={errors.description?.message}
          disabled={isRegistering}
          autoFocus
          {...register('description')}
        />
        <RegisterErrorAlert error={error} />
        <div className="flex flex-wrap items-center gap-3">
          <Button
            type="submit"
            variant="primary"
            loading={isRegistering}
            icon={<Sparkles className="size-4" aria-hidden />}
          >
            {isRegistering ? 'Asking AI…' : 'Register task'}
          </Button>
          {isRegistering && (
            <span className="text-sm text-muted">This usually takes a few seconds.</span>
          )}
        </div>
      </form>
    </Card>
  )
}
