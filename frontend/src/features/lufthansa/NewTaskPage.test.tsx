import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { todayIso } from '@/lib/date'
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
          reasoning: 'Opis dotyczy naprawy błędu w istniejącym kodzie.',
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
    // AI explains its pick
    expect(
      screen.getByText(/Opis dotyczy naprawy błędu w istniejącym kodzie\./),
    ).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/reporter/lufthansa/tasks:register')
    // the date field defaults to today and is sent along
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toEqual({
      createdAt: todayIso(),
      description: 'naprawa bledu w module raportow',
    })

    await user.click(screen.getByRole('button', { name: /Confirm & save/ }))

    expect(await screen.findByText('Task saved')).toBeInTheDocument()
    expect(fetchMock.mock.calls[1]?.[0]).toBe(`/reporter/lufthansa/tasks:complete/${TASK_ID}`)
    // a task AI classified itself is completed without a body
    expect(fetchMock.mock.calls[1]?.[1]?.body).toBeUndefined()
  })

  it('lets the user pick the category when AI could not classify the task', async () => {
    const reasoning = 'Opis jest zbyt ogólny — dopisz, co konkretnie robiłeś podczas hackatonu.'
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        json({
          id: TASK_ID,
          category: 'UNKNOWN',
          description: 'Uczestnictwo w hackatonie.',
          reasoning,
        }),
      )
      .mockResolvedValueOnce(
        json({ category: 'SOFTWARE_DEVELOPMENT', description: 'Uczestnictwo w hackatonie.' }),
      )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'uczestnictwo w hackatonie')
    await user.click(screen.getByRole('button', { name: 'Register task' }))

    // the reasoning that used to live only in the backend log
    expect(await screen.findByText('AI could not assign a category')).toBeInTheDocument()
    expect(screen.getByText(reasoning)).toBeInTheDocument()

    const confirm = screen.getByRole('button', { name: /Confirm & save/ })
    expect(confirm).toBeDisabled()

    await user.selectOptions(screen.getByLabelText('Pick a category'), 'SOFTWARE_DEVELOPMENT')
    expect(confirm).toBeEnabled()
    await user.click(confirm)

    expect(await screen.findByText('Task saved')).toBeInTheDocument()
    expect(fetchMock.mock.calls[1]?.[0]).toBe(`/reporter/lufthansa/tasks:complete/${TASK_ID}`)
    expect(JSON.parse(String(fetchMock.mock.calls[1]?.[1]?.body))).toEqual({
      category: 'SOFTWARE_DEVELOPMENT',
    })
    // the saved screen shows what was really stored, not the UNKNOWN of the register answer
    expect(screen.getAllByText('SOFTWARE DEVELOPMENT').length).toBeGreaterThan(0)
  })

  it('shows a failed AI call inline and keeps the form', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      json(
        { status: 502, title: 'AI service error', description: 'Failed to categorize the task' },
        502,
      ),
    )
    const user = userEvent.setup()
    renderPage()

    await user.type(screen.getByLabelText('Description'), 'zupełnie niezwiązany tekst')
    await user.click(screen.getByRole('button', { name: 'Register task' }))

    expect(await screen.findByText('AI is unavailable')).toBeInTheDocument()
    expect(screen.getByText('Failed to categorize the task')).toBeInTheDocument()
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
