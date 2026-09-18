import { postJson, request } from './client'
import type {
  LufthansaCreateTaskRequest,
  LufthansaReportItem,
  LufthansaTaskResponse,
  ReportPeriod,
} from './types'

export const lufthansaApi = {
  registerTask: (body: LufthansaCreateTaskRequest) =>
    postJson<LufthansaTaskResponse>('/lufthansa/tasks:register', body),

  completeTask: (taskId: string) =>
    postJson<LufthansaTaskResponse>(`/lufthansa/tasks:complete/${encodeURIComponent(taskId)}`),

  /** Costs one OpenAI call per category — call only on explicit user action. */
  getReport: ({ year, month }: ReportPeriod) =>
    request<LufthansaReportItem[]>(`/lufthansa/report?year=${year}&month=${month}`),
}
