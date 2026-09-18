import { deleteResource, postJson, putJson, request } from './client'
import type {
  ReportPeriod,
  TrecomCreateTaskRequest,
  TrecomReportItem,
  TrecomTask,
  TrecomTaskResponse,
  TrecomUpdateTaskRequest,
} from './types'

export const trecomApi = {
  registerTask: (body: TrecomCreateTaskRequest) =>
    postJson<TrecomTaskResponse>('/trecom/tasks:register', body),

  completeTask: (taskId: string) =>
    postJson<TrecomTaskResponse>(`/trecom/tasks:complete/${encodeURIComponent(taskId)}`),

  getTasks: ({ year, month }: ReportPeriod) =>
    request<TrecomTask[]>(`/trecom/tasks?year=${year}&month=${month}`),

  /** Manual correction — the backend does not run AI on updates. */
  updateTask: (id: number, body: TrecomUpdateTaskRequest) =>
    putJson<TrecomTask>(`/trecom/tasks/${id}`, body),

  deleteTask: (id: number) => deleteResource(`/trecom/tasks/${id}`),

  getReport: ({ year, month }: ReportPeriod) =>
    request<TrecomReportItem[]>(`/trecom/report?year=${year}&month=${month}`),
}
