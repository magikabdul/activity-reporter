import { describe, expect, it } from 'vitest'
import { trecomEditSchema, trecomTaskSchema } from './schema'

const valid = {
  createdAt: '2026-09-10',
  customer: 'Orlen',
  description: 'Konfiguracja klastra firewalli',
  hoursSpent: 4,
  salesman: { firstName: 'Łukasz', lastName: 'Żak-Nowak' },
  notes: '',
}

function messagesFor(input: unknown): string[] {
  const result = trecomTaskSchema.safeParse(input)
  return result.success ? [] : result.error.issues.map((issue) => issue.path.join('.'))
}

describe('trecomTaskSchema', () => {
  it('accepts a valid task including Polish diacritics and a hyphenated last name', () => {
    expect(trecomTaskSchema.safeParse(valid).success).toBe(true)
  })

  it.each([
    ['lowercase first name', { salesman: { firstName: 'anna', lastName: 'Nowak' } }],
    ['first name with digits', { salesman: { firstName: 'Anna1', lastName: 'Nowak' } }],
    ['two-word first name', { salesman: { firstName: 'Anna Maria', lastName: 'Nowak' } }],
  ])('rejects %s', (_, patch) => {
    expect(messagesFor({ ...valid, ...patch })).toEqual(['salesman.firstName'])
  })

  it.each([
    ['lowercase last name', 'nowak'],
    ['double hyphen', 'Nowak-Kowalska-Zielińska'],
    ['trailing hyphen', 'Nowak-'],
  ])('rejects %s', (_, lastName) => {
    expect(messagesFor({ ...valid, salesman: { firstName: 'Anna', lastName } })).toEqual([
      'salesman.lastName',
    ])
  })

  it('mirrors the backend limits for description and hours', () => {
    expect(messagesFor({ ...valid, description: 'too short' })).toEqual(['description'])
    expect(messagesFor({ ...valid, description: 'x'.repeat(256) })).toEqual(['description'])
    expect(messagesFor({ ...valid, hoursSpent: 0 })).toEqual(['hoursSpent'])
    expect(messagesFor({ ...valid, hoursSpent: 1.5 })).toEqual(['hoursSpent'])
    expect(messagesFor({ ...valid, hoursSpent: Number.NaN })).toEqual(['hoursSpent'])
  })

  it('requires a date that is not in the future', () => {
    expect(messagesFor({ ...valid, createdAt: '' })).toEqual(['createdAt'])
    expect(messagesFor({ ...valid, createdAt: '2999-01-01' })).toEqual(['createdAt'])
  })

  it('allows longer descriptions when editing (AI-corrected text may exceed the create limit)', () => {
    const description = 'x'.repeat(400)
    expect(trecomTaskSchema.safeParse({ ...valid, description }).success).toBe(false)
    expect(trecomEditSchema.safeParse({ ...valid, description }).success).toBe(true)
    expect(trecomEditSchema.safeParse({ ...valid, description: 'x'.repeat(501) }).success).toBe(
      false,
    )
  })

  it('requires a customer', () => {
    expect(messagesFor({ ...valid, customer: '   ' })).toEqual(['customer'])
  })
})
