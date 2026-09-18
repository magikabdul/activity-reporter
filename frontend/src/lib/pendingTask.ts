import { readJson, remove, writeJson } from './storage'
import type { CompanyId } from '@/theme/companies'

/**
 * A registered-but-not-completed task. The backend keeps exactly one such task per company in memory
 * and persists it only on `tasks:complete`, so we keep its id (and what the user typed) across reloads.
 */
export interface PendingTask<TInput, TResult> {
  input: TInput
  result: TResult & { id: string }
}

const key = (company: CompanyId) => `reporter.${company}.pendingTask`

export function loadPendingTask<TInput, TResult>(
  company: CompanyId,
): PendingTask<TInput, TResult> | null {
  return readJson<PendingTask<TInput, TResult> | null>(sessionStorage, key(company), null)
}

export function savePendingTask<TInput, TResult>(
  company: CompanyId,
  task: PendingTask<TInput, TResult>,
): void {
  writeJson(sessionStorage, key(company), task)
}

export function clearPendingTask(company: CompanyId): void {
  remove(sessionStorage, key(company))
}
