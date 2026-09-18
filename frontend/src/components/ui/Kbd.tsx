import type { ReactNode } from 'react'

/** Keyboard shortcut hint, e.g. <Kbd>Ctrl</Kbd> <Kbd>Enter</Kbd>. */
export function Kbd({ children }: { children: ReactNode }) {
  return (
    <kbd className="rounded border border-border bg-surface-2 px-1.5 py-0.5 font-sans text-[0.6875rem] font-medium text-muted">
      {children}
    </kbd>
  )
}

export function ShortcutHint({ keys, label }: { keys: string[]; label?: string }) {
  return (
    <span className="no-print hidden items-center gap-1 text-xs text-muted sm:inline-flex">
      {keys.map((key) => (
        <Kbd key={key}>{key}</Kbd>
      ))}
      {label && <span className="ml-0.5">{label}</span>}
    </span>
  )
}
