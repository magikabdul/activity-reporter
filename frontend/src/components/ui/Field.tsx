import { CalendarDays } from 'lucide-react'
import {
  useId,
  useRef,
  type InputHTMLAttributes,
  type ReactNode,
  type Ref,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { cn } from '@/lib/cn'
import { formatIsoDate } from '@/lib/date'

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

type SelectFieldProps = CommonProps &
  SelectHTMLAttributes<HTMLSelectElement> & {
    ref?: Ref<HTMLSelectElement>
    options: { value: string; label: string }[]
    /** explanatory text under the control, e.g. what the chosen option means */
    help?: ReactNode
  }

export function SelectField({
  label,
  error,
  hint,
  optional,
  wrapperClassName,
  options,
  help,
  className,
  ref,
  ...props
}: SelectFieldProps) {
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
      <select
        id={id}
        ref={ref}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={cn(CONTROL, 'h-10 cursor-pointer', className)}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {help && <p className="text-xs leading-relaxed text-muted">{help}</p>}
    </FieldShell>
  )
}

type DateFieldProps = CommonProps &
  Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value'> & {
    ref?: Ref<HTMLInputElement>
    /** current ISO value (yyyy-mm-dd), shown formatted */
    value: string
  }

/**
 * The native date input renders its text in the OS short-date format, which the app cannot control
 * (a Windows format like "ddd, dd.MM.yyyy" shows up in Chrome as ", 03.09.2026"). So the value is
 * displayed by us, in one format everywhere, and the native input only lends its calendar popup.
 */
export function DateField({
  label,
  error,
  hint,
  optional,
  wrapperClassName,
  value,
  disabled,
  ref,
  ...props
}: DateFieldProps) {
  const id = useId()
  const inputRef = useRef<HTMLInputElement | null>(null)

  function openPicker() {
    const input = inputRef.current
    if (!input) return
    try {
      input.showPicker()
    } catch {
      // no showPicker() (older browsers): at least hand over to the native control
      input.focus()
    }
  }

  return (
    <FieldShell
      id={id}
      label={label}
      error={error}
      hint={hint}
      optional={optional}
      className={wrapperClassName}
    >
      <div className="relative">
        <button
          type="button"
          id={id}
          disabled={disabled}
          onClick={openPicker}
          aria-haspopup="dialog"
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          className={cn(
            CONTROL,
            'flex h-10 cursor-pointer items-center justify-between gap-2 text-left whitespace-nowrap disabled:cursor-not-allowed',
          )}
        >
          <span className={cn('tabular-nums', !value && 'text-muted/70')}>
            {value ? formatIsoDate(value, { weekday: true }) : 'Pick a date'}
          </span>
          <CalendarDays className="size-4 shrink-0 text-muted" aria-hidden />
        </button>
        {/* holds the form value and anchors the calendar popup under the button */}
        <input
          type="date"
          tabIndex={-1}
          aria-hidden
          disabled={disabled}
          className="pointer-events-none absolute bottom-0 left-0 h-px w-px opacity-0"
          ref={(element) => {
            inputRef.current = element
            if (typeof ref === 'function') ref(element)
            else if (ref) ref.current = element
          }}
          {...props}
        />
      </div>
    </FieldShell>
  )
}
