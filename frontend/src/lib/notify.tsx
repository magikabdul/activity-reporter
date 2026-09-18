import { toast } from 'sonner'
import { Toast } from '@/components/ui/Toast'
import type { Tone } from '@/components/ui/tones'

// Success confirms and leaves; an error stays until the user has seen it.
const DURATION: Record<Tone, number | undefined> = {
  success: 4000,
  info: 6000,
  danger: undefined,
}

function show(tone: Tone, title: string, description?: string) {
  const duration = DURATION[tone]
  return toast.custom(
    (id) => (
      <Toast
        tone={tone}
        title={title}
        description={description}
        duration={duration}
        onClose={() => toast.dismiss(id)}
      />
    ),
    { duration: duration ?? Infinity },
  )
}

/**
 * Result of an action (saved, deleted, copied, exported…).
 * A problem that belongs to a form or a page goes into an inline <Alert> instead.
 */
export const notify = {
  success: (title: string, description?: string) => show('success', title, description),
  info: (title: string, description?: string) => show('info', title, description),
  error: (title: string, description?: string) => show('danger', title, description),
}
