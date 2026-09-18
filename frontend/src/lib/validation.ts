import { z } from 'zod'
import { todayIso } from './date'

// Both companies' CreateTaskRequest use @Size(min = 10, max = 255) on the description…
export const DESCRIPTION_MIN = 10
export const DESCRIPTION_MAX = 255
// …while UpdateTaskRequest allows 500: AI-corrected text may already be longer than what create accepts.
export const DESCRIPTION_MAX_EDIT = 500

export function descriptionSchema(max: number = DESCRIPTION_MAX) {
  return z
    .string()
    .trim()
    .min(DESCRIPTION_MIN, `Description must have at least ${DESCRIPTION_MIN} characters`)
    .max(max, `Description must have at most ${max} characters`)
}

/** yyyy-mm-dd, not in the future (the backend rejects future dates as well). */
export const createdAtSchema = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'Pick a date')
  .refine((value) => value <= todayIso(), 'Date can not be in the future')
