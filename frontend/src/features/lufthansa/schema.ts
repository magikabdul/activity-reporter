import { z } from 'zod'
import { descriptionSchema } from '@/lib/validation'

export const lufthansaTaskSchema = z.object({
  description: descriptionSchema,
})

export type LufthansaTaskForm = z.infer<typeof lufthansaTaskSchema>
