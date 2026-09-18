import { z } from 'zod'
import { ASSIGNABLE_TASK_CATEGORIES } from '@/api/types'
import { createdAtSchema, DESCRIPTION_MAX_EDIT, descriptionSchema } from '@/lib/validation'

export const lufthansaTaskSchema = z.object({
  createdAt: createdAtSchema,
  description: descriptionSchema(),
})

export type LufthansaTaskForm = z.infer<typeof lufthansaTaskSchema>

export const lufthansaEditSchema = z.object({
  createdAt: createdAtSchema,
  category: z.enum(ASSIGNABLE_TASK_CATEGORIES, 'Pick a category'),
  description: descriptionSchema(DESCRIPTION_MAX_EDIT),
})

export type LufthansaEditForm = z.infer<typeof lufthansaEditSchema>
