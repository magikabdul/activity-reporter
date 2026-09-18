import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles } from 'lucide-react'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { trecomApi } from '@/api/trecom'
import type { Salesman, TrecomCreateTaskRequest, TrecomTaskResponse } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { TextAreaField, TextField } from '@/components/ui/Field'
import { Steps } from '@/features/tasks/Steps'
import { RegisterErrorAlert, StaleTaskAlert } from '@/features/tasks/TaskAlerts'
import { TaskReview } from '@/features/tasks/TaskReview'
import { TaskSaved } from '@/features/tasks/TaskSaved'
import { useTaskFlow } from '@/features/tasks/useTaskFlow'
import { loadSuggestions, rememberTask, type TrecomSuggestions } from '@/lib/suggestions'
import { DESCRIPTION_MAX } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { trecomTaskSchema, type TrecomTaskForm } from './schema'

const company = COMPANIES.trecom
const RECENT_SALESMEN = 5

const EMPTY_FORM: TrecomTaskForm = {
  customer: '',
  description: '',
  hoursSpent: 1,
  salesman: { firstName: '', lastName: '' },
  notes: '',
}

function toRequest(values: TrecomTaskForm): TrecomCreateTaskRequest {
  const { notes, ...rest } = values
  return notes ? { ...rest, notes } : rest
}

export function TrecomNewTaskPage() {
  const [suggestions, setSuggestions] = useState(loadSuggestions)

  const flow = useTaskFlow<TrecomCreateTaskRequest, TrecomTaskResponse>({
    company: company.id,
    register: trecomApi.registerTask,
    complete: trecomApi.completeTask,
    onSaved: ({ result }) => {
      setSuggestions(rememberTask(result.customer, result.salesman))
      toast.success('Trecom task saved')
    },
  })

  const pending = flow.pending

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="New task"
        description="Log work done for a customer. AI validates the salesman's name and corrects the description before anything is stored."
      />
      <Steps current={flow.step} />

      {flow.step === 'form' && (
        <div className="flex flex-col gap-4">
          {flow.staleTask && <StaleTaskAlert />}
          <TaskForm
            key={flow.draft ? JSON.stringify(flow.draft) : 'empty'}
            defaultValues={flow.draft ? { ...EMPTY_FORM, ...flow.draft } : EMPTY_FORM}
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
            ...(pending.input.notes ? [{ label: 'Notes', result: pending.input.notes }] : []),
          ]}
          footnote={
            pending.input.notes
              ? 'Heads-up: the backend currently does not persist notes (known backend issue) — only the fields above it are saved.'
              : undefined
          }
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

function fullName({ firstName, lastName }: Salesman): string {
  return `${firstName} ${lastName}`
}

interface TaskFormProps {
  defaultValues: TrecomTaskForm
  suggestions: TrecomSuggestions
  isRegistering: boolean
  error: unknown
  onSubmit: (values: TrecomTaskForm) => void
}

function TaskForm({ defaultValues, suggestions, isRegistering, error, onSubmit }: TaskFormProps) {
  const {
    register,
    handleSubmit,
    control,
    setValue,
    formState: { errors },
  } = useForm<TrecomTaskForm>({
    resolver: zodResolver(trecomTaskSchema),
    defaultValues,
  })
  const length = useWatch({ control, name: 'description' }).trim().length
  const recentSalesmen = suggestions.salesmen.slice(0, RECENT_SALESMEN)

  function pickSalesman(salesman: Salesman) {
    setValue('salesman.firstName', salesman.firstName, { shouldValidate: true, shouldDirty: true })
    setValue('salesman.lastName', salesman.lastName, { shouldValidate: true, shouldDirty: true })
  }

  return (
    <Card>
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        <div className="grid gap-5 sm:grid-cols-[1fr_9rem]">
          <TextField
            label="Customer"
            placeholder="e.g. ORLEN"
            suggestions={suggestions.customers}
            error={errors.customer?.message}
            disabled={isRegistering}
            autoFocus
            {...register('customer')}
          />
          <TextField
            label="Hours spent"
            type="number"
            inputMode="numeric"
            min={1}
            step={1}
            error={errors.hoursSpent?.message}
            disabled={isRegistering}
            {...register('hoursSpent', { valueAsNumber: true })}
          />
        </div>

        <TextAreaField
          label="Description"
          placeholder="e.g. Konfiguracja i wdrożenie klastra firewalli w siedzibie klienta…"
          hint={`${length} / ${DESCRIPTION_MAX}`}
          error={errors.description?.message}
          disabled={isRegistering}
          {...register('description')}
        />

        <fieldset className="flex flex-col gap-3">
          <legend className="sr-only">Salesman</legend>
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              label="Salesman first name"
              placeholder="e.g. Anna"
              suggestions={[...new Set(suggestions.salesmen.map((s) => s.firstName))]}
              error={errors.salesman?.firstName?.message}
              disabled={isRegistering}
              {...register('salesman.firstName')}
            />
            <TextField
              label="Salesman last name"
              placeholder="e.g. Kowalska-Nowak"
              suggestions={[...new Set(suggestions.salesmen.map((s) => s.lastName))]}
              error={errors.salesman?.lastName?.message}
              disabled={isRegistering}
              {...register('salesman.lastName')}
            />
          </div>
          {recentSalesmen.length > 0 && (
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs text-muted">Recent:</span>
              {recentSalesmen.map((salesman) => (
                <Button
                  key={fullName(salesman)}
                  size="sm"
                  disabled={isRegistering}
                  onClick={() => pickSalesman(salesman)}
                >
                  {fullName(salesman)}
                </Button>
              ))}
            </div>
          )}
        </fieldset>

        <TextAreaField
          label="Notes"
          optional
          className="min-h-20"
          error={errors.notes?.message}
          disabled={isRegistering}
          {...register('notes')}
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
