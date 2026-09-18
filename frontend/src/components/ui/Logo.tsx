import { cn } from '@/lib/cn'

/**
 * The Reporter mark: a report sheet with a folded corner and a rising bar chart.
 * Drawn with theme tokens, so it takes the accent of whichever company page is open.
 * Keep `public/favicon.svg` in sync (same geometry, neutral palette).
 */
export function Logo({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      role="img"
      aria-label="Reporter"
      className={cn('size-8 shrink-0', className)}
    >
      <rect width="32" height="32" rx="9" className="fill-accent" />
      {/* sheet with the top-right corner cut off */}
      <path
        d="M10.75 6h7.1L24 12.15V24.25A1.75 1.75 0 0 1 22.25 26h-11.5A1.75 1.75 0 0 1 9 24.25V7.75A1.75 1.75 0 0 1 10.75 6Z"
        className="fill-accent-fg"
      />
      {/* the folded corner */}
      <path
        d="M17.85 6 24 12.15h-4.4a1.75 1.75 0 0 1-1.75-1.75V6Z"
        className="fill-accent"
        opacity="0.6"
      />
      {/* rising bars */}
      <rect x="11.6" y="19.2" width="2.5" height="4" rx="0.7" className="fill-accent" />
      <rect x="15.25" y="16.4" width="2.5" height="6.8" rx="0.7" className="fill-accent" />
      <rect x="18.9" y="14" width="2.5" height="9.2" rx="0.7" className="fill-accent" />
    </svg>
  )
}
