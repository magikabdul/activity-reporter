import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import { cn } from '@/lib/cn'
import { Button } from './Button'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  children: ReactNode
  /** blocks Esc / backdrop / close button, e.g. while a request is in flight */
  busy?: boolean
  size?: 'sm' | 'lg'
}

/**
 * Modal built on the native <dialog>: `showModal()` gives the focus trap, the top layer, inert
 * background and Esc handling for free.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  busy = false,
  size = 'lg',
}: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) {
      dialog.showModal()
      // showModal() focuses the first focusable element (our close button) and ignores React's
      // autoFocus, so move focus to where the user starts: a marked element or the first field.
      dialog
        .querySelector<HTMLElement>('[data-autofocus], input:not([aria-hidden]), select, textarea')
        ?.focus()
    }
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      // Esc: keep the dialog open while busy, otherwise let the parent own the state
      onCancel={(event) => {
        event.preventDefault()
        if (!busy) onClose()
      }}
      // a click on the <dialog> element itself can only be the backdrop
      onClick={(event) => {
        if (event.target === ref.current && !busy) onClose()
      }}
      className={cn(
        'm-auto w-[calc(100vw-2rem)] rounded-card border border-border bg-surface p-0 text-text shadow-2xl shadow-black/60',
        'backdrop:bg-black/70 backdrop:backdrop-blur-sm',
        size === 'sm' ? 'max-w-md' : 'max-w-2xl',
      )}
    >
      {open && (
        <div className="flex max-h-[calc(100dvh-4rem)] flex-col">
          <header className="flex items-start justify-between gap-4 border-b border-border px-5 py-4 sm:px-6">
            <div className="min-w-0">
              <h2 id={titleId} className="text-base font-semibold text-text">
                {title}
              </h2>
              {description && <p className="mt-1 text-sm text-muted">{description}</p>}
            </div>
            <Button
              variant="ghost"
              size="sm"
              aria-label="Close"
              disabled={busy}
              onClick={onClose}
              className="-mr-2 px-2"
              icon={<X className="size-4" aria-hidden />}
            />
          </header>
          <div className="overflow-y-auto px-5 py-5 sm:px-6">{children}</div>
        </div>
      )}
    </dialog>
  )
}

interface ConfirmDialogProps {
  open: boolean
  title: string
  children: ReactNode
  confirmLabel: string
  busy?: boolean
  onConfirm: () => void
  onClose: () => void
}

/** Replaces window.confirm (which blocks the page and cannot be themed). */
export function ConfirmDialog({
  open,
  title,
  children,
  confirmLabel,
  busy,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} title={title} busy={busy} size="sm">
      <div className="text-sm leading-relaxed text-muted">{children}</div>
      <div className="mt-6 flex flex-wrap justify-end gap-3">
        {/* initial focus on the safe choice */}
        <Button disabled={busy} onClick={onClose} data-autofocus>
          Cancel
        </Button>
        <Button variant="danger" loading={busy} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Dialog>
  )
}
