import { Menu, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { themeForPath } from '@/theme/companies'
import { Sidebar } from './Sidebar'

export function AppShell() {
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const theme = themeForPath(pathname)

  // The palette lives on <html> so the page background and scrollbars follow the company too.
  useEffect(() => {
    document.documentElement.dataset.company = theme
  }, [theme])

  return (
    <div className="min-h-dvh lg:pl-60 print:pl-0">
      <header className="no-print sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-border bg-surface/90 px-4 backdrop-blur lg:hidden">
        <button
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          className="flex size-9 cursor-pointer items-center justify-center rounded-control text-muted hover:bg-surface-2 hover:text-text"
        >
          {menuOpen ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
        <span className="text-sm font-semibold tracking-[0.2em]">REPORTER</span>
      </header>

      {menuOpen && (
        <button
          type="button"
          aria-label="Close menu"
          onClick={() => setMenuOpen(false)}
          className="no-print fixed inset-0 z-30 bg-black/60 lg:hidden"
        />
      )}

      <aside
        className={cn(
          'no-print fixed inset-y-0 left-0 z-40 w-60 border-r border-border bg-surface transition-transform',
          menuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
        )}
      >
        <Sidebar onNavigate={() => setMenuOpen(false)} />
      </aside>

      <main className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-8 sm:py-10">
        <Outlet />
      </main>
    </div>
  )
}
