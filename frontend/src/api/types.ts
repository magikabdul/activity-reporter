// Mirrors the backend DTOs — see docs/backend-api.md

export interface ErrorMessage {
  status: number
  title?: string
  description?: string
}

/* ---------- Lufthansa ---------- */

/** Categories a stored task can have. */
export const ASSIGNABLE_TASK_CATEGORIES = [
  'SOFTWARE_DEVELOPMENT',
  'CONSULTING_AND_TRAINING',
  'DOCUMENTATION',
  'CODE_ANALYSIS_AND_REFINEMENT',
  'BUG_FIXING_AND_MAINTENANCE',
  'TECHNOLOGY_SELECTION',
  'ARCHITECTURE_DESIGN',
] as const

export type AssignableTaskCategory = (typeof ASSIGNABLE_TASK_CATEGORIES)[number]

/**
 * UNKNOWN is what AI answers for a task it could not classify: registration still succeeds, and the category is
 * then picked by hand in the review step. It is never persisted.
 */
export type TaskCategory = AssignableTaskCategory | 'UNKNOWN'

export interface LufthansaCreateTaskRequest {
  description: string
  /** ISO date (yyyy-mm-dd) the work was done; the backend uses today when omitted */
  createdAt?: string
}

export interface LufthansaTaskResponse {
  /** present only in the register response */
  id?: string
  createdAt?: string
  category: TaskCategory
  description: string
  /** register only: why AI picked this category, or what the description is missing when it is UNKNOWN */
  reasoning?: string
}

/** Optional body of tasks:complete — the category the user picked when AI answered UNKNOWN. */
export interface LufthansaCompleteTaskRequest {
  category: AssignableTaskCategory
}

/** A stored task — note the numeric database id, unlike the UUID of a registration. */
export interface LufthansaTask {
  id: number
  createdAt: string
  category: AssignableTaskCategory
  description: string
}

export type LufthansaUpdateTaskRequest = Omit<LufthansaTask, 'id'>

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
  /** ISO date (yyyy-mm-dd) the work was done; the backend uses today when omitted */
  createdAt?: string
}

export interface TrecomTaskResponse {
  /** present only in the register response */
  id?: string
  createdAt?: string
  customer: string
  description: string
  hoursSpent: number
  salesman: Salesman
  notes?: string
}

/** A stored task — note the numeric database id, unlike the UUID of a registration. */
export interface TrecomTask {
  id: number
  createdAt: string
  customer: string
  description: string
  hoursSpent: number
  salesman: Salesman
  notes?: string | null
}

export type TrecomUpdateTaskRequest = Omit<TrecomTask, 'id'>

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
