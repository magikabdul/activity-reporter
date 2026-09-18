import { FilePlus2, FileText, LayoutDashboard, type LucideIcon } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/cn'
import { COMPANY_LIST } from '@/theme/companies'

interface SidebarLinkProps {
  to: string
  icon: LucideIcon
  label: string
  end?: boolean
  onNavigate?: () => void
}

function SidebarLink({ to, icon: Icon, label, end, onNavigate }: SidebarLinkProps) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onNavigate}
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2.5 rounded-control px-3 py-2 text-sm font-medium transition',
          isActive ? 'bg-accent-soft text-accent' : 'text-muted hover:bg-surface-2 hover:text-text',
        )
      }
    >
      <Icon className="size-4 shrink-0" aria-hidden />
      {label}
    </NavLink>
  )
}

export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="flex h-full flex-col gap-6 p-4">
      <div className="flex items-center gap-2.5 px-2 pt-1">
        <span className="flex size-8 items-center justify-center rounded-control bg-accent text-sm font-bold text-accent-fg">
          R
        </span>
        <span className="text-sm font-semibold tracking-[0.2em] text-text">REPORTER</span>
      </div>

      <SidebarLink to="/" end icon={LayoutDashboard} label="Home" onNavigate={onNavigate} />

      {COMPANY_LIST.map((company) => (
        <section key={company.id} aria-label={company.name} className="flex flex-col gap-1">
          <h2 className="flex items-center gap-2 px-3 pb-1 text-xs font-semibold tracking-[0.15em] text-muted uppercase">
            {/* the dot always shows the company's own accent, whatever page is open */}
            <span data-company={company.id} className="size-2 rounded-full bg-accent" aria-hidden />
            {company.name}
          </h2>
          <SidebarLink
            to={`${company.basePath}/new-task`}
            icon={FilePlus2}
            label="New task"
            onNavigate={onNavigate}
          />
          <SidebarLink
            to={`${company.basePath}/report`}
            icon={FileText}
            label="Report"
            onNavigate={onNavigate}
          />
        </section>
      ))}
    </nav>
  )
}
