import type { ErrorResponse } from '../types/api'

export class ApiError extends Error {
  readonly status: number
  readonly body: ErrorResponse

  constructor(body: ErrorResponse) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.body = body
  }
}

function apiBase(): string {
  const base = import.meta.env.VITE_API_BASE_URL
  if (!base) {
    throw new Error('VITE_API_BASE_URL is not set')
  }
  return base.replace(/\/$/, '')
}

function devHeaders(): HeadersInit {
  const headers: HeadersInit = { Accept: 'application/json' }
  const devUserId = import.meta.env.VITE_DEV_USER_ID
  if (devUserId) {
    headers['X-Dev-User-Id'] = String(devUserId)
  }
  return headers
}

async function parseError(response: Response, path: string): Promise<ApiError> {
  let body: ErrorResponse
  try {
    body = (await response.json()) as ErrorResponse
  } catch {
    body = {
      timestamp: new Date().toISOString(),
      status: response.status,
      error: 'HTTP_ERROR',
      message: response.statusText || 'Request failed',
      path,
    }
  }
  return new ApiError(body)
}

export async function apiGet<T>(path: string, query?: Record<string, string | number | undefined>): Promise<T> {
  const url = new URL(`${apiBase()}${path.startsWith('/') ? path : `/${path}`}`)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') {
        url.searchParams.set(key, String(value))
      }
    }
  }
  const response = await fetch(url, { headers: devHeaders() })
  if (!response.ok) {
    throw await parseError(response, url.pathname)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const url = `${apiBase()}${path.startsWith('/') ? path : `/${path}`}`
  const response = await fetch(url, {
    method: 'POST',
    headers: { ...devHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw await parseError(response, path)
  }
  return response.json() as Promise<T>
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const url = `${apiBase()}${path.startsWith('/') ? path : `/${path}`}`
  const response = await fetch(url, {
    method: 'PUT',
    headers: { ...devHeaders(), 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw await parseError(response, path)
  }
  return response.json() as Promise<T>
}

export async function apiDelete(path: string): Promise<void> {
  const url = `${apiBase()}${path.startsWith('/') ? path : `/${path}`}`
  const response = await fetch(url, { method: 'DELETE', headers: devHeaders() })
  if (!response.ok) {
    throw await parseError(response, path)
  }
}