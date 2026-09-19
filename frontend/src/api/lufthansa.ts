import { deleteResource, postJson, putJson, request } from './client'
import type {
  LufthansaCompleteTaskRequest,
  LufthansaCreateTaskRequest,
  LufthansaReportItem,
  LufthansaTask,
  LufthansaTaskResponse,
  LufthansaUpdateTaskRequest,
  ReportPeriod,
} from './types'

export const lufthansaApi = {
  registerTask: (body: LufthansaCreateTaskRequest) =>
    postJson<LufthansaTaskResponse>('/lufthansa/tasks:register', body),

  /** The body is sent only when AI answered UNKNOWN and the user picked the category themselves. */
  completeTask: (taskId: string, body?: LufthansaCompleteTaskRequest) =>
    postJson<LufthansaTaskResponse>(
      `/lufthansa/tasks:complete/${encodeURIComponent(taskId)}`,
      body,
    ),

  getTasks: ({ year, month }: ReportPeriod) =>
    request<LufthansaTask[]>(`/lufthansa/tasks?year=${year}&month=${month}`),

  /** Manual correction — the backend does not run AI on updates. */
  updateTask: (id: number, body: LufthansaUpdateTaskRequest) =>
    putJson<LufthansaTask>(`/lufthansa/tasks/${id}`, body),

  deleteTask: (id: number) => deleteResource(`/lufthansa/tasks/${id}`),

  /** Costs one OpenAI call per category — call only on explicit user action. */
  getReport: ({ year, month }: ReportPeriod) =>
    request<LufthansaReportItem[]>(`/lufthansa/report?year=${year}&month=${month}`),
}
