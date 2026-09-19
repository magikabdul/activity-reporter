import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { ApiError } from '@/api/client'
import {
  clearPendingTask,
  loadPendingTask,
  savePendingTask,
  type PendingTask,
} from '@/lib/pendingTask'
import type { CompanyId } from '@/theme/companies'

interface TaskFlowOptions<TInput, TResult, TExtra> {
  company: CompanyId
  register: (input: TInput) => Promise<TResult>
  /** `extra` carries what the user decided in the review step - Lufthansa sends a hand-picked category there. */
  complete: (taskId: string, extra?: TExtra) => Promise<Partial<TResult>>
  onSaved?: (task: PendingTask<TInput, TResult>) => void
}

export type TaskFlowStep = 'form' | 'review' | 'saved'

/**
 * register → review → complete.
 * The backend keeps the registered task only in memory (one per company) until `complete` persists it,
 * so "Edit again" simply abandons it — the next register overwrites it.
 */
export function useTaskFlow<TInput, TResult extends { id?: string }, TExtra = void>({
  company,
  register,
  complete,
  onSaved,
}: TaskFlowOptions<TInput, TResult, TExtra>) {
  const [pending, setPending] = useState(() => loadPendingTask<TInput, TResult>(company))
  const [saved, setSaved] = useState<PendingTask<TInput, TResult> | null>(null)
  const [draft, setDraft] = useState<TInput | null>(null)
  const [staleTask, setStaleTask] = useState(false)

  const registerMutation = useMutation({
    mutationFn: async (input: TInput) => {
      const result = await register(input)
      if (!result.id) throw new Error('The backend did not return a task id.')
      return { input, result: { ...result, id: result.id } }
    },
    onSuccess: (task) => {
      savePendingTask(company, task)
      setPending(task)
      setStaleTask(false)
    },
  })

  const completeMutation = useMutation({
    mutationFn: ({ task, extra }: { task: PendingTask<TInput, TResult>; extra?: TExtra }) =>
      complete(task.result.id, extra),
    onSuccess: (completed, { task }) => {
      // the complete response is the stored row - it carries what the backend really saved
      // (e.g. the category the user picked by hand, where the register result still says UNKNOWN)
      const saved: PendingTask<TInput, TResult> = {
        input: task.input,
        result: { ...task.result, ...completed },
      }
      clearPendingTask(company)
      setPending(null)
      setDraft(null)
      setSaved(saved)
      onSaved?.(saved)
    },
    onError: (error, { task }) => {
      // 400 = the in-memory task is gone (pod restart or overwritten by another register).
      if (error instanceof ApiError && error.status === 400) {
        clearPendingTask(company)
        setPending(null)
        setDraft(task.input)
        setStaleTask(true)
      }
    },
  })

  const step: TaskFlowStep = saved ? 'saved' : pending ? 'review' : 'form'

  return {
    step,
    pending,
    saved,
    /** values to pre-fill the form with after "Edit again" */
    draft,
    /** true when `complete` failed because the backend no longer holds the task */
    staleTask,
    registerError: registerMutation.error,
    completeError: staleTask ? null : completeMutation.error,
    isRegistering: registerMutation.isPending,
    isCompleting: completeMutation.isPending,

    submit: (input: TInput) => {
      setStaleTask(false)
      registerMutation.mutate(input)
    },
    confirm: (extra?: TExtra) => {
      if (pending) completeMutation.mutate({ task: pending, extra })
    },
    editAgain: () => {
      if (!pending) return
      clearPendingTask(company)
      setDraft(pending.input)
      setPending(null)
      registerMutation.reset()
      completeMutation.reset()
    },
    startNew: () => {
      setSaved(null)
      setDraft(null)
      registerMutation.reset()
      completeMutation.reset()
    },
  }
}
