import type { ApiErrorBody, SessionEvent, SessionResponse } from '../utils/types'

const BASE_URL = '/api'

export class ApiError extends Error {
  readonly status: number
  readonly code: string | null
  readonly traceId: string | null

  constructor(status: number, code: string | null, message: string, traceId: string | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.traceId = traceId
  }
}

async function throwApiError(response: Response): Promise<never> {
  const body: ApiErrorBody | null = await response.json().catch(() => null)
  const message = body?.message ?? `Request failed (${response.status})`
  throw new ApiError(response.status, body?.code ?? null, message, body?.traceId ?? null)
}

export async function createSession(): Promise<SessionResponse> {
  const response = await fetch(`${BASE_URL}/sessions`, { method: 'POST' })
  if (!response.ok) {
    return throwApiError(response)
  }
  return response.json()
}

export async function startSimulation(sessionId: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/sessions/${sessionId}/simulate`, { method: 'POST' })
  if (!response.ok) {
    return throwApiError(response)
  }
}

/** Rebuilds the full session view — used both to refresh while the SSE stream is down and to
 * rehydrate on demand, since GET /sessions/{id} is sufficient on its own. */
export async function getSession(sessionId: string): Promise<SessionResponse> {
  const response = await fetch(`${BASE_URL}/sessions/${sessionId}`)
  if (!response.ok) {
    return throwApiError(response)
  }
  return response.json()
}

export function subscribeToSession(
  sessionId: string,
  onEvent: (event: SessionEvent) => void,
  onError: () => void,
  onOpen: () => void,
): EventSource {
  const eventSource = new EventSource(`${BASE_URL}/sessions/${sessionId}/events`)

  eventSource.addEventListener('update', (message) => {
    onEvent(JSON.parse((message as MessageEvent<string>).data) as SessionEvent)
  })
  eventSource.addEventListener('failure', (message) => {
    onEvent(JSON.parse((message as MessageEvent<string>).data) as SessionEvent)
  })
  eventSource.onopen = onOpen
  eventSource.onerror = onError

  return eventSource
}
