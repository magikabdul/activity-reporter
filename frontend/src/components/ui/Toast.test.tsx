import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { toast } from 'sonner'
import { describe, expect, it, vi } from 'vitest'
import { notify } from '@/lib/notify'
import { Toast } from './Toast'

describe('Toast', () => {
  it('announces a success politely and shows the auto-dismiss timer', () => {
    const { container } = render(
      <Toast
        tone="success"
        title="Task updated"
        description="ORLEN · 4 h"
        duration={4000}
        onClose={() => {}}
      />,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Task updated')
    expect(screen.getByText('ORLEN · 4 h')).toBeInTheDocument()
    expect(container.querySelector('.toast-timer')).toHaveStyle({ animationDuration: '4000ms' })
  })

  it('announces an error assertively and has no timer, so it stays until dismissed', async () => {
    const onClose = vi.fn()
    const { container } = render(<Toast tone="danger" title="Delete failed" onClose={onClose} />)

    expect(screen.getByRole('alert')).toHaveTextContent('Delete failed')
    expect(container.querySelector('.toast-timer')).toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Dismiss notification' }))
    expect(onClose).toHaveBeenCalledOnce()
  })
})

describe('notify', () => {
  it('lets successes expire but keeps errors open', () => {
    const custom = vi.spyOn(toast, 'custom').mockReturnValue('id')

    notify.success('Saved')
    notify.error('Failed', 'Task not found')

    expect(custom.mock.calls[0]?.[1]).toEqual({ duration: 4000 })
    expect(custom.mock.calls[1]?.[1]).toEqual({ duration: Infinity })
  })
})
