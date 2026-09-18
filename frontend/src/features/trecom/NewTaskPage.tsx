import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { trecomApi } from '@/api/trecom'
import type { TrecomCreateTaskRequest, TrecomTaskResponse } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { ShortcutHint } from '@/components/ui/Kbd'
import { Steps } from '@/features/tasks/Steps'
import { RegisterErrorAlert, StaleTaskAlert } from '@/features/tasks/TaskAlerts'
import { TaskReview } from '@/features/tasks/TaskReview'
import { TaskSaved } from '@/features/tasks/TaskSaved'
import { useTaskChangeSync } from '@/features/tasks/useTaskChangeSync'
import { useTaskFlow } from '@/features/tasks/useTaskFlow'
import { formatIsoDate, todayIso } from '@/lib/date'
import { notify } from '@/lib/notify'
import { loadSuggestions, rememberTask, type TrecomSuggestions } from '@/lib/suggestions'
import { useSubmitShortcut } from '@/lib/useShortcut'
import { DESCRIPTION_MAX } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { fullName } from './salesman'
import { trecomTaskSchema, type TrecomTaskForm } from './schema'
import { TrecomTaskFields } from './TaskFields'

const company = COMPANIES.trecom

function emptyForm(): TrecomTaskForm {
  return {
    createdAt: todayIso(),
    customer: '',
    description: '',
    hoursSpent: 1,
    salesman: { firstName: '', lastName: '' },
    notes: '',
  }
}

function toRequest(values: TrecomTaskForm): TrecomCreateTaskRequest {
  const { notes, ...rest } = values
  return notes ? { ...rest, notes } : rest
}

export function TrecomNewTaskPage() {
  const [suggestions, setSuggestions] = useState(loadSuggestions)
  const syncTaskChange = useTaskChangeSync(company.id)

  const flow = useTaskFlow<TrecomCreateTaskRequest, TrecomTaskResponse>({
    company: company.id,
    register: trecomApi.registerTask,
    complete: trecomApi.completeTask,
    onSaved: ({ input, result }) => {
      setSuggestions(rememberTask(result.customer, result.salesman))
      syncTaskChange(result.createdAt ?? input.createdAt ?? todayIso())
      notify.success('Task saved', `${result.customer} · ${result.hoursSpent} h`)
    },
  })

  const pending = flow.pending

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="New task"
        description="Log work done for a customer. AI validates the salesman's name and corrects the description and notes before anything is stored."
      />
      <Steps current={flow.step} />

      {flow.step === 'form' && (
        <div className="flex flex-col gap-4">
          {flow.staleTask && <StaleTaskAlert />}
          <TaskForm
            key={flow.draft ? JSON.stringify(flow.draft) : 'empty'}
            defaultValues={flow.draft ? { ...emptyForm(), ...flow.draft } : emptyForm()}
            suggestions={suggestions}
            isRegistering={flow.isRegistering}
            error={flow.registerError}
            onSubmit={(values) => flow.submit(toRequest(values))}
          />
        </div>
      )}

      {flow.step === 'review' && pending && (
        <TaskReview
          rows={[
            {
              label: 'Date',
              result: formatIsoDate(
                pending.result.createdAt ?? pending.input.createdAt ?? todayIso(),
              ),
            },
            {
              label: 'Customer',
              input: pending.input.customer,
              result: pending.result.customer,
              changed: pending.input.customer !== pending.result.customer,
            },
            {
              label: 'Description',
              input: pending.input.description,
              result: pending.result.description,
              changed: pending.input.description !== pending.result.description,
            },
            { label: 'Hours spent', result: `${pending.result.hoursSpent} h` },
            {
              label: 'Salesman',
              input: fullName(pending.input.salesman),
              result: fullName(pending.result.salesman),
              changed: fullName(pending.input.salesman) !== fullName(pending.result.salesman),
            },
            ...(pending.input.notes
              ? [
                  {
                    label: 'Notes',
                    input: pending.input.notes,
                    // a backend older than 1.1.0 does not return (or store) notes
                    result: pending.result.notes ?? pending.input.notes,
                    changed:
                      pending.result.notes != null && pending.result.notes !== pending.input.notes,
                  },
                ]
              : []),
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
              <p className="font-medium text-text">
                {flow.saved.result.customer} · {flow.saved.result.hoursSpent} h ·{' '}
                {fullName(flow.saved.result.salesman)}
              </p>
              <p className="mt-1">{flow.saved.result.description}</p>
            </>
          }
        />
      )}
    </>
  )
}

interface TaskFormProps {
  defaultValues: TrecomTaskForm
  suggestions: TrecomSuggestions
  isRegistering: boolean
  error: unknown
  onSubmit: (values: TrecomTaskForm) => void
}

function TaskForm({ defaultValues, suggestions, isRegistering, error, onSubmit }: TaskFormProps) {
  const form = useForm<TrecomTaskForm>({
    resolver: zodResolver(trecomTaskSchema),
    defaultValues,
  })
  const submit = form.handleSubmit(onSubmit)
  useSubmitShortcut(() => void submit(), !isRegistering)

  return (
    <Card>
      <form onSubmit={submit} noValidate className="flex flex-col gap-5">
        <TrecomTaskFields
          form={form}
          suggestions={suggestions}
          descriptionMax={DESCRIPTION_MAX}
          disabled={isRegistering}
          autoFocus
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
