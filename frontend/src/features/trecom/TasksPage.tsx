import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { trecomApi } from '@/api/trecom'
import type { TrecomTask, TrecomUpdateTaskRequest } from '@/api/types'
import { Card } from '@/components/ui/Card'
import { EditFormActions } from '@/features/tasks/EditFormActions'
import {
  MonthlyTasksView,
  type EditFormProps,
  type TaskColumn,
} from '@/features/tasks/MonthlyTasksView'
import { formatIsoDate } from '@/lib/date'
import { loadSuggestions, rememberTask } from '@/lib/suggestions'
import { useSubmitShortcut } from '@/lib/useShortcut'
import { DESCRIPTION_MAX_EDIT } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { fullName } from './salesman'
import { trecomEditSchema, type TrecomTaskForm } from './schema'
import { TrecomTaskFields } from './TaskFields'

const COLUMNS: TaskColumn<TrecomTask>[] = [
  {
    id: 'createdAt',
    label: 'Date',
    sortValue: (task) => task.createdAt,
    render: (task) => formatIsoDate(task.createdAt),
    className: 'whitespace-nowrap tabular-nums',
  },
  {
    id: 'customer',
    label: 'Customer',
    sortValue: (task) => task.customer,
    render: (task) => task.customer,
    className: 'font-medium',
  },
  {
    id: 'description',
    label: 'Description',
    sortValue: (task) => task.description,
    render: (task) => (
      <>
        {task.description}
        {task.notes && <p className="mt-1 text-xs text-muted">Notes: {task.notes}</p>}
      </>
    ),
    className: 'min-w-64 text-text/90',
  },
  {
    id: 'salesman',
    label: 'Salesman',
    sortValue: (task) => fullName(task.salesman),
    render: (task) => fullName(task.salesman),
    className: 'whitespace-nowrap',
  },
  {
    id: 'hoursSpent',
    label: 'Hours',
    align: 'right',
    sortValue: (task) => task.hoursSpent,
    render: (task) => task.hoursSpent,
  },
]

const describe = (task: TrecomTask) =>
  `${formatIsoDate(task.createdAt)} · ${task.customer} · ${task.hoursSpent} h`

export function TrecomTasksPage() {
  return (
    <MonthlyTasksView<TrecomTask, TrecomUpdateTaskRequest>
      company={COMPANIES.trecom}
      description="Everything logged in the selected month. Review it and fix the customer, hours, salesman or date before you use the report."
      columns={COLUMNS}
      list={trecomApi.getTasks}
      update={trecomApi.updateTask}
      remove={trecomApi.deleteTask}
      describe={describe}
      summary={(tasks) => (
        <Card className="flex flex-wrap items-baseline gap-x-8 gap-y-2 py-4 sm:py-4">
          <Total label="Tasks" value={tasks.length} />
          <Total
            label="Total hours"
            value={`${tasks.reduce((sum, task) => sum + task.hoursSpent, 0)} h`}
          />
        </Card>
      )}
      renderEditForm={(props) => <EditForm key={props.task.id} {...props} />}
    />
  )
}

function Total({ label, value }: { label: string; value: string | number }) {
  return (
    <p className="text-sm text-muted">
      {label} <span className="ml-1.5 text-lg font-semibold text-text tabular-nums">{value}</span>
    </p>
  )
}

function EditForm({
  task,
  busy,
  error,
  onSubmit,
  onCancel,
}: EditFormProps<TrecomTask, TrecomUpdateTaskRequest>) {
  const [suggestions, setSuggestions] = useState(loadSuggestions)
  const form = useForm<TrecomTaskForm>({
    resolver: zodResolver(trecomEditSchema),
    defaultValues: {
      createdAt: task.createdAt,
      customer: task.customer,
      description: task.description,
      hoursSpent: task.hoursSpent,
      salesman: task.salesman,
      notes: task.notes ?? '',
    },
  })

  const submit = form.handleSubmit(({ notes, ...values }) => {
    setSuggestions(rememberTask(values.customer, values.salesman))
    // an emptied notes field clears the stored notes
    onSubmit({ ...values, notes: notes || null })
  })
  useSubmitShortcut(() => void submit(), !busy)

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <TrecomTaskFields
        form={form}
        suggestions={suggestions}
        descriptionMax={DESCRIPTION_MAX_EDIT}
        disabled={busy}
      />
      <EditFormActions busy={busy} error={error} onCancel={onCancel} />
    </form>
  )
}
