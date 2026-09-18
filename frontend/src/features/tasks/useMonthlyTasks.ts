import { useMutation, useQuery } from '@tanstack/react-query'
import type { ReportPeriod } from '@/api/types'
import { notify } from '@/lib/notify'
import { periodKey } from '@/lib/period'
import type { CompanyId } from '@/theme/companies'
import { tasksQueryKey, useTaskChangeSync } from './useTaskChangeSync'

export interface StoredTask {
  id: number
  createdAt: string
}

interface MonthlyTasksOptions<TTask extends StoredTask, TUpdate> {
  company: CompanyId
  period: ReportPeriod
  list: (period: ReportPeriod) => Promise<TTask[]>
  update: (id: number, body: TUpdate) => Promise<TTask>
  remove: (id: number) => Promise<void>
  /** short text identifying a task in a toast, e.g. "ORLEN · 4 h" */
  describe: (task: TTask) => string
}

export function useMonthlyTasks<TTask extends StoredTask, TUpdate>({
  company,
  period,
  list,
  update,
  remove,
  describe,
}: MonthlyTasksOptions<TTask, TUpdate>) {
  const syncTaskChange = useTaskChangeSync(company)

  const query = useQuery({
    queryKey: tasksQueryKey(company, periodKey(period)),
    queryFn: () => list(period),
    staleTime: 30_000,
  })

  // Failures of both mutations are shown inline in their dialog, successes as a toast.
  const updateMutation = useMutation({
    mutationFn: ({ task, values }: { task: TTask; values: TUpdate }) => update(task.id, values),
    onSuccess: (updated, { task }) => {
      // the edit may have moved the task to another month
      syncTaskChange(task.createdAt, updated.createdAt)
      notify.success('Task updated', describe(updated))
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (task: TTask) => remove(task.id),
    onSuccess: (_, task) => {
      syncTaskChange(task.createdAt)
      notify.success('Task deleted', describe(task))
    },
  })

  return { query, updateMutation, deleteMutation }
}
