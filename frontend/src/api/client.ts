import type { ErrorMessage } from './types'

export const API_BASE = '/reporter'

export class ApiError extends Error {
  readonly status: number
  readonly title: string
  /** true when the backend sent an ErrorMessage body (e.g. AI errors arrive as 404 *with* a body) */
  readonly hasBody: boolean

  constructor(status: number, title: string, description: string, hasBody: boolean) {
    super(description)
    this.name = 'ApiError'
    this.status = status
    this.title = title
    this.hasBody = hasBody
  }

  /** The backend answers an empty report with a body-less 404. */
  get isEmptyResult(): boolean {
    return this.status === 404 && !this.hasBody
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  const text = await response.text().catch(() => '')
  if (text) {
    try {
      const body = JSON.parse(text) as ErrorMessage
      return new ApiError(
        response.status,
        body.title ?? response.statusText,
        body.description ?? body.title ?? `Request failed with status ${response.status}`,
        true,
      )
    } catch {
      // not JSON (e.g. an nginx error page) — fall through
    }
  }
  return new ApiError(
    response.status,
    response.statusText || 'Request failed',
    `Request failed with status ${response.status}`,
    false,
  )
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new ApiError(0, 'Network error', 'The backend is unreachable.', false)
  }

  if (!response.ok) throw await toApiError(response)
  // 204 No Content (e.g. DELETE) has no body to parse
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function postJson<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function deleteResource(path: string): Promise<void> {
  return request<void>(path, { method: 'DELETE' })
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  return 'Unexpected error'
}
