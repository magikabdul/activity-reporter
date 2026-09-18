import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { lufthansaApi } from '@/api/lufthansa'
import {
  ASSIGNABLE_TASK_CATEGORIES,
  type LufthansaTask,
  type LufthansaUpdateTaskRequest,
} from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { SelectField } from '@/components/ui/Field'
import { EditFormActions } from '@/features/tasks/EditFormActions'
import { DescriptionField, TaskDateField } from '@/features/tasks/fields'
import {
  MonthlyTasksView,
  type EditFormProps,
  type TaskColumn,
} from '@/features/tasks/MonthlyTasksView'
import { formatIsoDate } from '@/lib/date'
import { useSubmitShortcut } from '@/lib/useShortcut'
import { DESCRIPTION_MAX_EDIT } from '@/lib/validation'
import { COMPANIES } from '@/theme/companies'
import { CATEGORY_DESCRIPTIONS, categoryLabel } from './categories'
import { lufthansaEditSchema, type LufthansaEditForm } from './schema'

const COLUMNS: TaskColumn<LufthansaTask>[] = [
  {
    id: 'createdAt',
    label: 'Date',
    sortValue: (task) => task.createdAt,
    render: (task) => formatIsoDate(task.createdAt),
    className: 'whitespace-nowrap tabular-nums',
  },
  {
    id: 'category',
    label: 'Category',
    sortValue: (task) => task.category,
    render: (task) => <Badge>{categoryLabel(task.category)}</Badge>,
  },
  {
    id: 'description',
    label: 'Description',
    sortValue: (task) => task.description,
    render: (task) => task.description,
    className: 'min-w-64 text-text/90',
  },
]

const CATEGORY_OPTIONS = ASSIGNABLE_TASK_CATEGORIES.map((category) => ({
  value: category,
  label: categoryLabel(category),
}))

const describe = (task: LufthansaTask) =>
  `${formatIsoDate(task.createdAt)} · ${categoryLabel(task.category)}`

export function LufthansaTasksPage() {
  return (
    <MonthlyTasksView<LufthansaTask, LufthansaUpdateTaskRequest>
      company={COMPANIES.lufthansa}
      description="Everything saved in the selected month. Review it and fix the wording, category or date before you generate the report."
      columns={COLUMNS}
      list={lufthansaApi.getTasks}
      update={lufthansaApi.updateTask}
      remove={lufthansaApi.deleteTask}
      describe={describe}
      renderEditForm={(props) => <EditForm key={props.task.id} {...props} />}
    />
  )
}

function EditForm({
  task,
  busy,
  error,
  onSubmit,
  onCancel,
}: EditFormProps<LufthansaTask, LufthansaUpdateTaskRequest>) {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<LufthansaEditForm>({
    resolver: zodResolver(lufthansaEditSchema),
    defaultValues: {
      createdAt: task.createdAt,
      category: task.category,
      description: task.description,
    },
  })
  const [category, description, createdAt] = useWatch({
    control,
    name: ['category', 'description', 'createdAt'],
  })
  const submit = handleSubmit(onSubmit)
  useSubmitShortcut(() => void submit(), !busy)

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-5">
      <div className="grid gap-5 sm:grid-cols-[12rem_1fr]">
        <TaskDateField
          registration={register('createdAt')}
          value={createdAt}
          error={errors.createdAt?.message}
          disabled={busy}
        />
        <SelectField
          label="Category"
          options={CATEGORY_OPTIONS}
          error={errors.category?.message}
          disabled={busy}
          {...register('category')}
        />
      </div>
      <p className="-mt-2 text-xs leading-relaxed text-muted">{CATEGORY_DESCRIPTIONS[category]}</p>
      <DescriptionField
        registration={register('description')}
        value={description}
        max={DESCRIPTION_MAX_EDIT}
        error={errors.description?.message}
        disabled={busy}
        autoFocus
      />
      <EditFormActions busy={busy} error={error} onCancel={onCancel} />
    </form>
  )
}
