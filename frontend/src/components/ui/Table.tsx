import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import type { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from 'react'
import { cn } from '@/lib/cn'

export function Table({ className, ...props }: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="overflow-x-auto rounded-control border border-border">
      <table className={cn('w-full border-collapse text-left text-sm', className)} {...props} />
    </div>
  )
}

export type SortDirection = 'asc' | 'desc'

interface ThProps extends ThHTMLAttributes<HTMLTableCellElement> {
  align?: 'left' | 'right'
  sorted?: SortDirection | null
  onSort?: () => void
}

export function Th({ align = 'left', sorted, onSort, className, children, ...props }: ThProps) {
  const Icon = sorted === 'asc' ? ArrowUp : sorted === 'desc' ? ArrowDown : ArrowUpDown
  return (
    <th
      scope="col"
      aria-sort={sorted === 'asc' ? 'ascending' : sorted === 'desc' ? 'descending' : undefined}
      className={cn(
        'border-b border-border bg-surface-2 px-3.5 py-2.5 text-xs font-semibold tracking-wide text-muted uppercase',
        align === 'right' && 'text-right',
        className,
      )}
      {...props}
    >
      {onSort ? (
        <button
          type="button"
          onClick={onSort}
          className={cn(
            'inline-flex cursor-pointer items-center gap-1 uppercase hover:text-text',
            sorted && 'text-accent',
          )}
        >
          {children}
          <Icon className="no-print size-3" aria-hidden />
        </button>
      ) : (
        children
      )}
    </th>
  )
}

interface TdProps extends TdHTMLAttributes<HTMLTableCellElement> {
  align?: 'left' | 'right'
}

export function Td({ align = 'left', className, ...props }: TdProps) {
  return (
    <td
      className={cn(
        'border-b border-border/60 px-3.5 py-3 align-top text-text [tr:last-child>&]:border-b-0',
        align === 'right' && 'text-right tabular-nums',
        className,
      )}
      {...props}
    />
  )
}
