import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { LufthansaNewTaskPage } from './NewTaskPage'

const TASK_ID = '7b0c1a52-6a55-4f0e-9d8e-3f3f0c1d2e4f'

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })

function renderPage() {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LufthansaNewTaskPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('LufthansaNewTaskPage', () => {
  it('validates the description before calling the backend', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'short')
    await user.click(screen.getByRole('button', { name: 'Register task' }))

    expect(await screen.findByText(/at least 10 characters/)).toBeInTheDocument()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('registers, shows the AI result and saves only after confirmation', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        json({
          id: TASK_ID,
          category: 'BUG_FIXING_AND_MAINTENANCE',
          description: 'Naprawa błędu w module raportów.',
        }),
      )
      .mockResolvedValueOnce(
        json({
          category: 'BUG_FIXING_AND_MAINTENANCE',
          description: 'Naprawa błędu w module raportów.',
        }),
      )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'naprawa bledu w module raportow')
    await user.click(screen.getByRole('button', { name: 'Register task' }))

    // review step: category + corrected text, nothing saved yet
    expect(await screen.findByText('BUG FIXING AND MAINTENANCE')).toBeInTheDocument()
    expect(screen.getByText('Naprawa błędu w module raportów.')).toBeInTheDocument()
    expect(screen.getByText('naprawa bledu w module raportow')).toBeInTheDocument()
    expect(screen.getByText('corrected')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/reporter/lufthansa/tasks:register')

    await user.click(screen.getByRole('button', { name: /Confirm & save/ }))

    expect(await screen.findByText('Task saved')).toBeInTheDocument()
    expect(fetchMock.mock.calls[1]?.[0]).toBe(`/reporter/lufthansa/tasks:complete/${TASK_ID}`)
  })

  it('shows an AI rejection inline and keeps the form', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json(
        { status: 404, title: 'AI processing error', description: 'Task could not be classified' },
        404,
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'zupełnie niezwiązany tekst')
    await user.click(screen.getByRole('button', { name: 'Register task' }))

    expect(await screen.findByText('AI could not accept this task')).toBeInTheDocument()
    expect(screen.getByText('Task could not be classified')).toBeInTheDocument()
    expect(screen.getByLabelText('Description')).toHaveValue('zupełnie niezwiązany tekst')
  })

  it('"Edit again" returns to the form with the original input', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json({ id: TASK_ID, category: 'DOCUMENTATION', description: 'Poprawiony opis zadania.' }),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'poprawiony opis zadania')
    await user.click(screen.getByRole('button', { name: 'Register task' }))
    await user.click(await screen.findByRole('button', { name: 'Edit again' }))

    expect(screen.getByLabelText('Description')).toHaveValue('poprawiony opis zadania')
  })

  it('restores the input when the backend no longer holds the task', async () => {
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        json({ id: TASK_ID, category: 'DOCUMENTATION', description: 'Opis zadania po korekcie.' }),
      )
      .mockResolvedValueOnce(
        json({ status: 400, title: 'Task processing error', description: 'Task not found' }, 400),
      )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'opis zadania przed korekta')
    await user.click(screen.getByRole('button', { name: 'Register task' }))
    await user.click(await screen.findByRole('button', { name: /Confirm & save/ }))

    expect(await screen.findByText('The registered task expired')).toBeInTheDocument()
    expect(screen.getByLabelText('Description')).toHaveValue('opis zadania przed korekta')
  })
})
