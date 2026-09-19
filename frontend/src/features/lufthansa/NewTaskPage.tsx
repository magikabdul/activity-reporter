import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { lufthansaApi } from '@/api/lufthansa'
import type {
  AssignableTaskCategory,
  LufthansaCompleteTaskRequest,
  LufthansaCreateTaskRequest,
  LufthansaTaskResponse,
} from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { SelectField } from '@/components/ui/Field'
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
import type { PendingTask } from '@/lib/pendingTask'
import { useSubmitShortcut } from '@/lib/useShortcut'
import { DESCRIPTION_MAX } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { CATEGORY_DESCRIPTIONS, CATEGORY_OPTIONS, categoryLabel } from './categories'
import { lufthansaTaskSchema, type LufthansaTaskForm } from './schema'

const company = COMPANIES.lufthansa

export function LufthansaNewTaskPage() {
  const syncTaskChange = useTaskChangeSync(company.id)

  const flow = useTaskFlow<
    LufthansaCreateTaskRequest,
    LufthansaTaskResponse,
    LufthansaCompleteTaskRequest
  >({
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
        <ReviewStep
          key={flow.pending.result.id}
          pending={flow.pending}
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

interface ReviewStepProps {
  pending: PendingTask<LufthansaCreateTaskRequest, LufthansaTaskResponse>
  error: unknown
  isCompleting: boolean
  onConfirm: (body?: LufthansaCompleteTaskRequest) => void
  onEdit: () => void
}

/**
 * AI answers UNKNOWN for a task it could not classify — instead of failing, it says what the description is
 * missing and the category is picked here by hand (the backend takes it in the `complete` body).
 */
function ReviewStep({ pending, error, isCompleting, onConfirm, onEdit }: ReviewStepProps) {
  const { input, result } = pending
  const needsCategory = result.category === 'UNKNOWN'
  const [picked, setPicked] = useState<AssignableTaskCategory | ''>('')

  return (
    <TaskReview
      notice={
        needsCategory && (
          <Alert tone="danger" title="AI could not assign a category">
            {result.reasoning ??
              'Pick the category yourself, or go back and describe the task in more detail.'}
          </Alert>
        )
      }
      rows={[
        {
          label: 'Date',
          result: formatIsoDate(result.createdAt ?? input.createdAt ?? todayIso()),
        },
        {
          label: 'Category',
          result: needsCategory ? (
            <SelectField
              label="Pick a category"
              options={[{ value: '', label: 'Choose a category…' }, ...CATEGORY_OPTIONS]}
              value={picked}
              onChange={(event) => setPicked(event.target.value as AssignableTaskCategory | '')}
              help={picked ? CATEGORY_DESCRIPTIONS[picked] : undefined}
              disabled={isCompleting}
              wrapperClassName="max-w-md"
            />
          ) : (
            <div className="flex flex-col items-start gap-2">
              <Badge>{categoryLabel(result.category)}</Badge>
              <span className="text-muted">{CATEGORY_DESCRIPTIONS[result.category]}</span>
            </div>
          ),
        },
        {
          label: 'Description',
          input: input.description,
          result: result.description,
          changed: input.description !== result.description,
        },
      ]}
      footnote={
        !needsCategory && result.reasoning ? `Why this category: ${result.reasoning}` : null
      }
      error={error}
      isCompleting={isCompleting}
      confirmDisabled={needsCategory && !picked}
      onConfirm={() => onConfirm(picked ? { category: picked } : undefined)}
      onEdit={onEdit}
    />
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
  const [description, createdAt] = useWatch({ control, name: ['description', 'createdAt'] })
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
            value={createdAt}
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
