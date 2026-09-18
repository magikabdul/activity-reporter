import { z } from 'zod'

// Both companies' CreateTaskRequest use @Size(min = 10, max = 255) on the description.
export const DESCRIPTION_MIN = 10
export const DESCRIPTION_MAX = 255

export const descriptionSchema = z
  .string()
  .trim()
  .min(DESCRIPTION_MIN, `Description must have at least ${DESCRIPTION_MIN} characters`)
  .max(DESCRIPTION_MAX, `Description must have at most ${DESCRIPTION_MAX} characters`)
