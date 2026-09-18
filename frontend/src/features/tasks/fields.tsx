import type { UseFormRegisterReturn } from 'react-hook-form'
import { DateField, TextAreaField } from '@/components/ui/Field'
import { todayIso } from '@/lib/date'

// Fields every task form shares (create and edit, both companies) — labels, hints and
// constraints live here once.

interface FieldProps {
  registration: UseFormRegisterReturn
  error?: string
  disabled?: boolean
}

interface TaskDateFieldProps extends FieldProps {
  /** current value, shown formatted */
  value: string
}

export function TaskDateField({ registration, error, disabled, value }: TaskDateFieldProps) {
  return (
    <DateField
      label="Date"
      value={value}
      max={todayIso()}
      error={error}
      disabled={disabled}
      {...registration}
    />
  )
}

interface DescriptionFieldProps extends FieldProps {
  /** current value, for the counter */
  value: string
  max: number
  placeholder?: string
  autoFocus?: boolean
}

export function DescriptionField({
  registration,
  error,
  disabled,
  value,
  max,
  placeholder,
  autoFocus,
}: DescriptionFieldProps) {
  return (
    <TextAreaField
      label="Description"
      placeholder={placeholder}
      hint={`${value.trim().length} / ${max}`}
      error={error}
      disabled={disabled}
      autoFocus={autoFocus}
      // <Dialog> moves the initial focus here (showModal() ignores autoFocus)
      {...(autoFocus ? { 'data-autofocus': true } : {})}
      {...registration}
    />
  )
}
