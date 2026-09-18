import { z } from 'zod'
import {
  createdAtSchema,
  DESCRIPTION_MAX,
  DESCRIPTION_MAX_EDIT,
  descriptionSchema,
} from '@/lib/validation'

// Same patterns as the backend's Salesman DTO (unicode-aware, so "Łukasz" / "Żak-Nowak" pass).
const FIRST_NAME = /^\p{Lu}\p{Ll}*$/u
const LAST_NAME = /^\p{Lu}\p{Ll}*(-\p{Lu}\p{Ll}*)?$/u

function taskSchema(descriptionMax: number) {
  return z.object({
    createdAt: createdAtSchema,
    customer: z
      .string()
      .trim()
      .min(1, 'Customer is required')
      .max(100, 'Customer must have at most 100 characters'),
    description: descriptionSchema(descriptionMax),
    hoursSpent: z
      .number({ error: 'Hours must be a number' })
      .int('Hours must be a whole number')
      .min(1, 'Hours must be at least 1')
      .max(9999, 'Hours must be at most 9999'),
    salesman: z.object({
      firstName: z
        .string()
        .trim()
        .min(1, 'First name is required')
        .max(100)
        .regex(FIRST_NAME, 'Must start with an uppercase letter and contain only letters'),
      lastName: z
        .string()
        .trim()
        .min(1, 'Last name is required')
        .max(100)
        .regex(
          LAST_NAME,
          'Must start with an uppercase letter and contain only letters (one hyphen allowed)',
        ),
    }),
    notes: z.string().trim().max(2000, 'Notes must have at most 2000 characters').optional(),
  })
}

/** Create and edit share every field; only the description limit differs (see lib/validation.ts). */
export const trecomTaskSchema = taskSchema(DESCRIPTION_MAX)
export const trecomEditSchema = taskSchema(DESCRIPTION_MAX_EDIT)

export type TrecomTaskForm = z.infer<typeof trecomTaskSchema>
