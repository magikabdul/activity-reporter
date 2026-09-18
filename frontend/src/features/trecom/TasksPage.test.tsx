import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { TrecomTask } from '@/api/types'
import { TrecomTasksPage } from './TasksPage'

const TASK: TrecomTask = {
  id: 7,
  createdAt: '2026-09-11',
  customer: 'AMS',
  description: 'Wsparcie przy Magic Info',
  hoursSpent: 1,
  salesman: { firstName: 'Łukasz', lastName: 'Milosch' },
  notes: null,
}

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <TrecomTasksPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

/** Routes fetch by method so the order of background refetches does not matter. */
function mockApi(handlers: {
  put?: () => Response
  delete?: () => Response
  tasks?: TrecomTask[]
}) {
  let tasks = handlers.tasks ?? [TASK]
  const calls: { method: string; url: string; body?: unknown }[] = []

  vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const method = init?.method ?? 'GET'
    const body = init?.body ? (JSON.parse(String(init.body)) as TrecomTask) : undefined
    calls.push({ method, url: String(input), body })

    if (method === 'PUT' && handlers.put) {
      const response = handlers.put()
      if (response.ok && body)
        tasks = tasks.map((task) => (task.id === 7 ? { ...body, id: 7 } : task))
      return response.ok && body ? json({ ...body, id: 7 }) : response
    }
    if (method === 'DELETE' && handlers.delete) {
      const response = handlers.delete()
      if (response.ok) tasks = []
      return response
    }
    return json(tasks)
  })

  return calls
}

describe('TrecomTasksPage', () => {
  it('lists the tasks of the month with totals', async () => {
    mockApi({})
    renderPage()

    expect(await screen.findByText('Wsparcie przy Magic Info')).toBeInTheDocument()
    expect(screen.getByText('Łukasz Milosch')).toBeInTheDocument()
    expect(screen.getByText('Total hours')).toHaveTextContent('1 h')
  })

  it('shows an empty state for a month without tasks', async () => {
    mockApi({ tasks: [] })
    renderPage()

    expect(await screen.findByText(/^No tasks in /)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'New task' })).toHaveAttribute(
      'href',
      '/trecom/new-task',
    )
  })

  it('edits a task manually and saves it with PUT', async () => {
    const calls = mockApi({ put: () => json({}) })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^Edit task:/ }))
    const dialog = screen.getByRole('dialog', { name: 'Edit task' })

    const hours = within(dialog).getByLabelText('Hours spent')
    await user.clear(hours)
    await user.type(hours, '3')
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    await waitFor(() => expect(calls.some((call) => call.method === 'PUT')).toBe(true))
    const put = calls.find((call) => call.method === 'PUT')
    expect(put?.url).toBe('/reporter/trecom/tasks/7')
    expect(put?.body).toEqual({
      createdAt: '2026-09-11',
      customer: 'AMS',
      description: 'Wsparcie przy Magic Info',
      hoursSpent: 3,
      salesman: { firstName: 'Łukasz', lastName: 'Milosch' },
      notes: null,
    })

    // dialog closes and the refreshed list shows the new value
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Edit task' })).toBeNull())
    expect(await screen.findByText('Total hours')).toHaveTextContent('3 h')
  })

  it('validates the edit form before calling the backend', async () => {
    const calls = mockApi({ put: () => json({}) })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^Edit task:/ }))
    const dialog = screen.getByRole('dialog', { name: 'Edit task' })
    await user.clear(within(dialog).getByLabelText('Customer'))
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    expect(await within(dialog).findByText('Customer is required')).toBeInTheDocument()
    expect(calls.some((call) => call.method === 'PUT')).toBe(false)
  })

  it('keeps the dialog open and shows the backend error when saving fails', async () => {
    mockApi({
      put: () =>
        json(
          { status: 404, title: 'Task not found', description: 'Task with id 7 does not exist' },
          404,
        ),
    })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^Edit task:/ }))
    const dialog = screen.getByRole('dialog', { name: 'Edit task' })
    await user.click(within(dialog).getByRole('button', { name: 'Save changes' }))

    expect(await within(dialog).findByText('Saving failed')).toBeInTheDocument()
    expect(within(dialog).getByText('Task with id 7 does not exist')).toBeInTheDocument()
  })

  it('deletes a task only after confirmation', async () => {
    const calls = mockApi({ delete: () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^Delete task:/ }))
    const dialog = screen.getByRole('dialog', { name: 'Delete this task?' })
    expect(calls.some((call) => call.method === 'DELETE')).toBe(false)

    await user.click(within(dialog).getByRole('button', { name: 'Delete task' }))

    await waitFor(() =>
      expect(calls.find((call) => call.method === 'DELETE')?.url).toBe('/reporter/trecom/tasks/7'),
    )
    expect(await screen.findByText(/^No tasks in /)).toBeInTheDocument()
  })

  it('cancelling the confirmation deletes nothing', async () => {
    const calls = mockApi({ delete: () => new Response(null, { status: 204 }) })
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: /^Delete task:/ }))
    await user.click(
      within(screen.getByRole('dialog', { name: 'Delete this task?' })).getByRole('button', {
        name: 'Cancel',
      }),
    )

    expect(calls.some((call) => call.method === 'DELETE')).toBe(false)
    expect(screen.getByText('Wsparcie przy Magic Info')).toBeInTheDocument()
  })
})
