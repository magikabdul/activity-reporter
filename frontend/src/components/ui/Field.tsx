import {
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type Ref,
  type TextareaHTMLAttributes,
} from 'react'
import { cn } from '@/lib/cn'

const CONTROL =
  'w-full rounded-control border border-border bg-surface-2 px-3 text-sm text-text placeholder:text-muted/70 ' +
  'transition focus:border-accent focus:outline-none focus:ring-2 focus:ring-accent/30 ' +
  'aria-invalid:border-danger aria-invalid:focus:ring-danger/30 disabled:opacity-50'

interface FieldShellProps {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  optional?: boolean
  className?: string
  children: ReactNode
}

function FieldShell({ id, label, error, hint, optional, className, children }: FieldShellProps) {
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <div className="flex items-baseline justify-between gap-2">
        <label htmlFor={id} className="text-sm font-medium text-text">
          {label}
          {optional && <span className="ml-1.5 text-xs font-normal text-muted">optional</span>}
        </label>
        {hint && <span className="text-xs text-muted tabular-nums">{hint}</span>}
      </div>
      {children}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  )
}

interface CommonProps {
  label: string
  error?: string
  hint?: ReactNode
  optional?: boolean
  wrapperClassName?: string
}

type TextFieldProps = CommonProps &
  InputHTMLAttributes<HTMLInputElement> & {
    ref?: Ref<HTMLInputElement>
    /** values offered through a native datalist */
    suggestions?: string[]
  }

export function TextField({
  label,
  error,
  hint,
  optional,
  wrapperClassName,
  suggestions,
  className,
  ref,
  ...props
}: TextFieldProps) {
  const id = useId()
  const listId = suggestions?.length ? `${id}-list` : undefined

  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      optional={optional}
      className={wrapperClassName}
    >
      <input
        id={id}
        ref={ref}
        list={listId}
        autoComplete="off"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={cn(CONTROL, 'h-10', className)}
        {...props}
      />
      {listId && (
        <datalist id={listId}>
          {suggestions?.map((value) => (
            <option key={value} value={value} />
          ))}
        </datalist>
      )}
    </FieldShell>
  )
}

type TextAreaFieldProps = CommonProps &
  TextareaHTMLAttributes<HTMLTextAreaElement> & {
    ref?: Ref<HTMLTextAreaElement>
  }

export function TextAreaField({
  label,
  error,
  hint,
  optional,
  wrapperClassName,
  className,
  ref,
  ...props
}: TextAreaFieldProps) {
  const id = useId()

  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      optional={optional}
      className={wrapperClassName}
    >
      <textarea
        id={id}
        ref={ref}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={cn(CONTROL, 'min-h-28 resize-y py-2.5 leading-relaxed', className)}
        {...props}
      />
    </FieldShell>
  )
}
