// Mirrors the backend DTOs — see docs/backend-api.md

export interface ErrorMessage {
  status: number
  title?: string
  description?: string
}

/* ---------- Lufthansa ---------- */

export const TASK_CATEGORIES = [
  'SOFTWARE_DEVELOPMENT',
  'CONSULTING_AND_TRAINING',
  'DOCUMENTATION',
  'CODE_ANALYSIS_AND_REFINEMENT',
  'BUG_FIXING_AND_MAINTENANCE',
  'TECHNOLOGY_SELECTION',
  'ARCHITECTURE_DESIGN',
  'UNKNOWN',
] as const

export type TaskCategory = (typeof TASK_CATEGORIES)[number]

export interface LufthansaCreateTaskRequest {
  description: string
}

export interface LufthansaTaskResponse {
  /** present only in the register response */
  id?: string
  category: TaskCategory
  description: string
}

export interface LufthansaReportItem {
  name: string
  description: string
  summary: string
}

/* ---------- Trecom ---------- */

export interface Salesman {
  firstName: string
  lastName: string
}

export interface TrecomCreateTaskRequest {
  customer: string
  description: string
  hoursSpent: number
  salesman: Salesman
  notes?: string
}

export interface TrecomTaskResponse {
  /** present only in the register response */
  id?: string
  customer: string
  description: string
  hoursSpent: number
  salesman: Salesman
  notes?: string
}

export interface TrecomReportItem {
  createdAt: string
  company: string
  description: string
  salesman: string
  hoursSpent: number
}

export interface ReportPeriod {
  year: number
  month: number
}
