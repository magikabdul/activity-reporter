import { postJson, request } from './client'
import type {
  ReportPeriod,
  TrecomCreateTaskRequest,
  TrecomReportItem,
  TrecomTaskResponse,
} from './types'

export const trecomApi = {
  registerTask: (body: TrecomCreateTaskRequest) =>
    postJson<TrecomTaskResponse>('/trecom/tasks:register', body),

  completeTask: (taskId: string) =>
    postJson<TrecomTaskResponse>(`/trecom/tasks:complete/${encodeURIComponent(taskId)}`),

  getReport: ({ year, month }: ReportPeriod) =>
    request<TrecomReportItem[]>(`/trecom/report?year=${year}&month=${month}`),
}
