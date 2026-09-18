import { FilePlus2, FileSearch, FileText, Pencil, RefreshCw, Trash2 } from 'lucide-react'
import { useMemo, useState, type ReactNode } from 'react'
import { errorMessage } from '@/api/client'
import type { ReportPeriod } from '@/api/types'
import { PageHeader } from '@/components/layout/PageHeader'
import { MonthPicker } from '@/components/MonthPicker'
import { Alert } from '@/components/ui/Alert'
import { Button, ButtonLink } from '@/components/ui/Button'
import { ConfirmDialog, Dialog } from '@/components/ui/Dialog'
import { EmptyState } from '@/components/ui/EmptyState'
import { Skeleton } from '@/components/ui/Skeleton'
import { Table, Td, Th, type SortDirection } from '@/components/ui/Table'
import { currentPeriod, periodLabel } from '@/lib/period'
import type { Company } from '@/theme/companies'
import { useMonthlyTasks, type StoredTask } from './useMonthlyTasks'

export interface TaskColumn<TTask> {
  id: string
  label: string
  align?: 'right'
  /** value used for sorting */
  sortValue: (task: TTask) => string | number
  render: (task: TTask) => ReactNode
  className?: string
}

export interface EditFormProps<TTask, TUpdate> {
  task: TTask
  busy: boolean
  error: unknown
  onSubmit: (values: TUpdate) => void
  onCancel: () => void
}

interface MonthlyTasksViewProps<TTask extends StoredTask, TUpdate> {
  company: Company
  description: string
  columns: TaskColumn<TTask>[]
  list: (period: ReportPeriod) => Promise<TTask[]>
  update: (id: number, body: TUpdate) => Promise<TTask>
  remove: (id: number) => Promise<void>
  describe: (task: TTask) => string
  /** summary shown above the table, e.g. totals */
  summary?: (tasks: TTask[]) => ReactNode
  renderEditForm: (props: EditFormProps<TTask, TUpdate>) => ReactNode
}

/** Review the stored tasks of a month, correct or delete them — before the report is generated. */
export function MonthlyTasksView<TTask extends StoredTask, TUpdate>({
  company,
  description,
  columns,
  list,
  update,
  remove,
  describe,
  summary,
  renderEditForm,
}: MonthlyTasksViewProps<TTask, TUpdate>) {
  const [period, setPeriod] = useState(currentPeriod)
  const [sort, setSort] = useState<{ id: string; direction: SortDirection }>({
    id: columns[0]?.id ?? '',
    direction: 'asc',
  })
  const [editing, setEditing] = useState<TTask | null>(null)
  const [deleting, setDeleting] = useState<TTask | null>(null)

  const { query, updateMutation, deleteMutation } = useMonthlyTasks({
    company: company.id,
    period,
    list,
    update,
    remove,
    describe,
  })

  const tasks = query.data
  const sorted = useMemo(() => {
    const column = columns.find((candidate) => candidate.id === sort.id)
    if (!tasks || !column) return tasks ?? []
    const factor = sort.direction === 'asc' ? 1 : -1
    return [...tasks].sort((a, b) => {
      const left = column.sortValue(a)
      const right = column.sortValue(b)
      const result =
        typeof left === 'number' && typeof right === 'number'
          ? left - right
          : String(left).localeCompare(String(right), 'pl')
      return (result || a.id - b.id) * factor
    })
  }, [tasks, columns, sort])

  function toggleSort(id: string) {
    setSort((current) =>
      current.id === id
        ? { id, direction: current.direction === 'asc' ? 'desc' : 'asc' }
        : { id, direction: 'asc' },
    )
  }

  function closeEdit() {
    setEditing(null)
    updateMutation.reset()
  }

  function closeDelete() {
    setDeleting(null)
    deleteMutation.reset()
  }

  return (
    <>
      <PageHeader
        eyebrow={company.name}
        title="Tasks"
        description={description}
        actions={<MonthPicker value={period} onChange={setPeriod} />}
      />

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <Button
          loading={query.isFetching}
          icon={<RefreshCw className="size-4" aria-hidden />}
          onClick={() => query.refetch()}
        >
          Refresh
        </Button>
        <ButtonLink
          to={`${company.basePath}/report`}
          icon={<FileText className="size-4" aria-hidden />}
        >
          Open report
        </ButtonLink>
      </div>

      {query.isError ? (
        <Alert
          tone="danger"
          title="Could not load the tasks"
          action={
            <Button size="sm" onClick={() => query.refetch()}>
              Try again
            </Button>
          }
        >
          {errorMessage(query.error)}
        </Alert>
      ) : query.isPending ? (
        <Skeleton className="h-64" />
      ) : sorted.length === 0 ? (
        <EmptyState
          icon={FileSearch}
          title={`No tasks in ${periodLabel(period)}`}
          description={`Nothing was saved for ${company.name} in this month.`}
          action={
            <ButtonLink
              to={`${company.basePath}/new-task`}
              variant="primary"
              icon={<FilePlus2 className="size-4" aria-hidden />}
            >
              New task
            </ButtonLink>
          }
        />
      ) : (
        <div className="flex flex-col gap-6">
          {summary?.(sorted)}
          <Table>
            <thead>
              <tr>
                {columns.map((column) => (
                  <Th
                    key={column.id}
                    align={column.align}
                    sorted={sort.id === column.id ? sort.direction : null}
                    onSort={() => toggleSort(column.id)}
                  >
                    {column.label}
                  </Th>
                ))}
                <Th align="right">
                  <span className="sr-only">Actions</span>
                </Th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((task) => (
                <tr key={task.id} className="transition-colors hover:bg-surface">
                  {columns.map((column) => (
                    <Td key={column.id} align={column.align} className={column.className}>
                      {column.render(task)}
                    </Td>
                  ))}
                  <Td align="right" className="whitespace-nowrap">
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={`Edit task: ${describe(task)}`}
                      icon={<Pencil className="size-3.5" aria-hidden />}
                      onClick={() => setEditing(task)}
                    >
                      Edit
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={`Delete task: ${describe(task)}`}
                      className="hover:text-danger"
                      icon={<Trash2 className="size-3.5" aria-hidden />}
                      onClick={() => setDeleting(task)}
                    >
                      Delete
                    </Button>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      )}

      <Dialog
        open={editing !== null}
        onClose={closeEdit}
        busy={updateMutation.isPending}
        title="Edit task"
        description="Saved exactly as entered — AI is not involved in corrections."
      >
        {editing &&
          renderEditForm({
            task: editing,
            busy: updateMutation.isPending,
            error: updateMutation.error,
            onCancel: closeEdit,
            onSubmit: (values) =>
              updateMutation.mutate({ task: editing, values }, { onSuccess: closeEdit }),
          })}
      </Dialog>

      <ConfirmDialog
        open={deleting !== null}
        onClose={closeDelete}
        busy={deleteMutation.isPending}
        title="Delete this task?"
        confirmLabel="Delete task"
        onConfirm={() => deleting && deleteMutation.mutate(deleting, { onSuccess: closeDelete })}
      >
        {deleting && (
          <>
            <p className="font-medium text-text">{describe(deleting)}</p>
            <p className="mt-2">This removes the task from the database and cannot be undone.</p>
            {deleteMutation.error != null && (
              <Alert tone="danger" title="Delete failed" className="mt-4">
                {errorMessage(deleteMutation.error)}
              </Alert>
            )}
          </>
        )}
      </ConfirmDialog>
    </>
  )
}
