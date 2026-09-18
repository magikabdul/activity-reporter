import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { DateField } from './Field'

describe('DateField', () => {
  it('shows the value in the app format instead of the OS-dependent native rendering', () => {
    render(<DateField label="Date" value="2026-09-03" onChange={() => {}} />)

    expect(screen.getByRole('button', { name: 'Date' })).toHaveTextContent('Thu, 3 Sept 2026')
  })

  it('prompts for a date when empty', () => {
    render(<DateField label="Date" value="" onChange={() => {}} />)

    expect(screen.getByRole('button', { name: 'Date' })).toHaveTextContent('Pick a date')
  })

  it('opens the native calendar of the hidden input, which holds the form value and limits', async () => {
    const showPicker = vi.fn()
    HTMLInputElement.prototype.showPicker = showPicker
    const { container } = render(
      <DateField label="Date" value="2026-09-03" max="2026-09-18" name="createdAt" readOnly />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Date' }))

    expect(showPicker).toHaveBeenCalledOnce()
    const input = container.querySelector('input[type="date"]')
    expect(input).toHaveAttribute('name', 'createdAt')
    expect(input).toHaveAttribute('max', '2026-09-18')
    expect(input).toHaveAttribute('tabindex', '-1')
  })

  it('links the validation error to the visible control', () => {
    render(<DateField label="Date" value="" error="Pick a date" onChange={() => {}} />)

    const button = screen.getByRole('button', { name: 'Date' })
    expect(button).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('Pick a date')
  })
})
