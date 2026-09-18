import { ApiError, errorMessage } from '@/api/client'
import { Alert } from '@/components/ui/Alert'

/** The backend reports AI rejections as 404 *with* a body — present them as what they are. */
export function RegisterErrorAlert({ error }: { error: unknown }) {
  if (error == null) return null
  const aiRejected = error instanceof ApiError && error.status === 404 && error.hasBody
  const title = aiRejected
    ? 'AI could not accept this task'
    : error instanceof ApiError && error.status === 400
      ? 'The backend rejected the data'
      : 'Registration failed'

  return (
    <Alert tone="danger" title={title}>
      {errorMessage(error)}
    </Alert>
  )
}

export function StaleTaskAlert() {
  return (
    <Alert tone="danger" title="The registered task expired">
      The backend no longer holds this task (it was restarted or another task was registered in the
      meantime). Your input is restored below — register it again.
    </Alert>
  )
}
