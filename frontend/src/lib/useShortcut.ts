import { useEffect, useRef } from 'react'

/**
 * Ctrl+Enter (⌘+Enter on macOS) anywhere on the page — the "do the primary action" shortcut:
 * submit the form, confirm the save, save the dialog.
 */
export function useSubmitShortcut(action: () => void, enabled = true): void {
  // always call the latest closure without re-subscribing on every render
  const latest = useRef(action)
  useEffect(() => {
    latest.current = action
  })

  useEffect(() => {
    if (!enabled) return

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Enter' && (event.ctrlKey || event.metaKey) && !event.repeat) {
        event.preventDefault()
        latest.current()
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [enabled])
}
