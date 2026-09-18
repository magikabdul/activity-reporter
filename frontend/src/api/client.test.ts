import { describe, expect, it, vi } from 'vitest'
import { ApiError, deleteResource, postJson, putJson, request } from './client'

function mockFetch(response: Response | Error) {
  const fetchMock = vi
    .spyOn(globalThis, 'fetch')
    .mockImplementation(() =>
      response instanceof Error ? Promise.reject(response) : Promise.resolve(response),
    )
  return fetchMock
}

const json = (body: unknown, status: number) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })

describe('request', () => {
  it('prefixes the backend base path and returns parsed JSON', async () => {
    const fetchMock = mockFetch(json([{ name: 'DOCUMENTATION' }], 200))

    const result = await request('/lufthansa/report?year=2026&month=9')

    expect(result).toEqual([{ name: 'DOCUMENTATION' }])
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/reporter/lufthansa/report?year=2026&month=9')
  })

  it('sends a JSON body on POST and keeps the literal colon in the path', async () => {
    const fetchMock = mockFetch(json({ id: 'abc' }, 200))

    await postJson('/trecom/tasks:register', { customer: 'ACME' })

    const [url, init] = fetchMock.mock.calls[0] ?? []
    expect(url).toBe('/reporter/trecom/tasks:register')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe('{"customer":"ACME"}')
    expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json')
  })

  it('sends PUT with a JSON body', async () => {
    const fetchMock = mockFetch(json({ id: 7 }, 200))

    const result = await putJson('/trecom/tasks/7', { hoursSpent: 3 })

    expect(result).toEqual({ id: 7 })
    const [url, init] = fetchMock.mock.calls[0] ?? []
    expect(url).toBe('/reporter/trecom/tasks/7')
    expect(init?.method).toBe('PUT')
    expect(init?.body).toBe('{"hoursSpent":3}')
  })

  it('resolves DELETE answered with 204 No Content without parsing a body', async () => {
    const fetchMock = mockFetch(new Response(null, { status: 204 }))

    await expect(deleteResource('/lufthansa/tasks/7')).resolves.toBeUndefined()

    const [url, init] = fetchMock.mock.calls[0] ?? []
    expect(url).toBe('/reporter/lufthansa/tasks/7')
    expect(init?.method).toBe('DELETE')
  })

  it('maps a validation error body to ApiError', async () => {
    mockFetch(
      json(
        { status: 400, title: 'invalid request content', description: 'description size…' },
        400,
      ),
    )

    const error = await request('/x').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      title: 'invalid request content',
      message: 'description size…',
      hasBody: true,
      isEmptyResult: false,
    })
  })

  it('treats an AI error (404 with body) as a real error, not an empty result', async () => {
    mockFetch(
      json(
        { status: 404, title: 'AI processing error', description: 'Task could not be classified' },
        404,
      ),
    )

    const error = (await request('/x').catch((e: unknown) => e)) as ApiError

    expect(error.message).toBe('Task could not be classified')
    expect(error.isEmptyResult).toBe(false)
  })

  it('flags a body-less 404 as an empty result', async () => {
    mockFetch(new Response(null, { status: 404 }))

    const error = (await request('/x').catch((e: unknown) => e)) as ApiError

    expect(error.status).toBe(404)
    expect(error.isEmptyResult).toBe(true)
  })

  it('survives non-JSON error pages from the ingress', async () => {
    mockFetch(new Response('<html>502 Bad Gateway</html>', { status: 502 }))

    const error = (await request('/x').catch((e: unknown) => e)) as ApiError

    expect(error.status).toBe(502)
    expect(error.hasBody).toBe(false)
    expect(error.message).toBe('Request failed with status 502')
  })

  it('reports network failures', async () => {
    mockFetch(new TypeError('Failed to fetch'))

    const error = (await request('/x').catch((e: unknown) => e)) as ApiError

    expect(error.status).toBe(0)
    expect(error.message).toBe('The backend is unreachable.')
  })
})
