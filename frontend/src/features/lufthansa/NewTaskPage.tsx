import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useForm, useWatch } from 'react-hook-form'
import { lufthansaApi } from '@/api/lufthansa'
import type { LufthansaCreateTaskRequest, LufthansaTaskResponse } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { ShortcutHint } from '@/components/ui/Kbd'
import { DescriptionField, TaskDateField } from '@/features/tasks/fields'
import { Steps } from '@/features/tasks/Steps'
import { RegisterErrorAlert, StaleTaskAlert } from '@/features/tasks/TaskAlerts'
import { TaskReview } from '@/features/tasks/TaskReview'
import { TaskSaved } from '@/features/tasks/TaskSaved'
import { useTaskChangeSync } from '@/features/tasks/useTaskChangeSync'
import { useTaskFlow } from '@/features/tasks/useTaskFlow'
import { formatIsoDate, todayIso } from '@/lib/date'
import { notify } from '@/lib/notify'
import { useSubmitShortcut } from '@/lib/useShortcut'
import { DESCRIPTION_MAX } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { CATEGORY_DESCRIPTIONS, categoryLabel } from './categories'
import { lufthansaTaskSchema, type LufthansaTaskForm } from './schema'

const company = COMPANIES.lufthansa

export function LufthansaNewTaskPage() {
  const syncTaskChange = useTaskChangeSync(company.id)

  const flow = useTaskFlow<LufthansaCreateTaskRequest, LufthansaTaskResponse>({
    company: company.id,
    register: lufthansaApi.registerTask,
    complete: lufthansaApi.completeTask,
    onSaved: ({ input, result }) => {
      syncTaskChange(result.createdAt ?? input.createdAt ?? todayIso())
      notify.success('Task saved', categoryLabel(result.category))
    },
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
            key={flow.draft ? JSON.stringify(flow.draft) : 'empty'}
            defaultValues={{
              createdAt: flow.draft?.createdAt ?? todayIso(),
              description: flow.draft?.description ?? '',
            }}
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
              label: 'Date',
              result: formatIsoDate(
                flow.pending.result.createdAt ?? flow.pending.input.createdAt ?? todayIso(),
              ),
            },
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
          tasksPath={`${company.basePath}/tasks`}
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
  const description = useWatch({ control, name: 'description' })
  const submit = handleSubmit(onSubmit)
  useSubmitShortcut(() => void submit(), !isRegistering)

  return (
    <Card>
      <form onSubmit={submit} noValidate className="flex flex-col gap-5">
        <DescriptionField
          registration={register('description')}
          value={description}
          max={DESCRIPTION_MAX}
          placeholder="e.g. Analiza wymagań i implementacja endpointu do eksportu raportów…"
          error={errors.description?.message}
          disabled={isRegistering}
          autoFocus
        />
        <div className="sm:max-w-48">
          <TaskDateField
            registration={register('createdAt')}
            error={errors.createdAt?.message}
            disabled={isRegistering}
          />
        </div>
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
          {isRegistering ? (
            <span className="text-sm text-muted">This usually takes a few seconds.</span>
          ) : (
            <ShortcutHint keys={['Ctrl', 'Enter']} />
          )}
        </div>
      </form>
    </Card>
  )
}
