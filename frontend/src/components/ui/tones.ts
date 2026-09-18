import { CircleAlert, CircleCheck, Info, type LucideIcon } from 'lucide-react'

export type Tone = 'danger' | 'success' | 'info'

interface ToneStyle {
  icon: LucideIcon
  /** text colour of the icon / title */
  text: string
  /** tinted background + matching border, for boxed feedback (Alert) */
  box: string
  /** solid colour, for thin accents such as the toast's tone bar and timer */
  solid: string
}

/** One visual language for every piece of feedback — shared by <Alert> (inline) and <Toast> (floating). */
export const TONES: Record<Tone, ToneStyle> = {
  danger: {
    icon: CircleAlert,
    text: 'text-danger',
    box: 'border-danger/40 bg-danger-soft',
    solid: 'bg-danger',
  },
  success: {
    icon: CircleCheck,
    text: 'text-success',
    box: 'border-success/40 bg-success-soft',
    solid: 'bg-success',
  },
  info: {
    icon: Info,
    text: 'text-accent',
    box: 'border-accent/30 bg-accent-soft',
    solid: 'bg-accent',
  },
}
