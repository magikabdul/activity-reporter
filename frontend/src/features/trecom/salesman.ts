import type { Salesman } from '@/api/types'

export function fullName({ firstName, lastName }: Salesman): string {
  return `${firstName} ${lastName}`
}
