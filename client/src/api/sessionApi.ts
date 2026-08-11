import type { GameSession, SessionEvent } from '../game/types'

const BASE_URL = (import.meta.env.VITE_SESSION_SERVICE_URL as string | undefined) ?? 'http://localhost:8082'

export async function createSession(): Promise<GameSession> {
  const response = await fetch(`${BASE_URL}/sessions`, { method: 'POST' })
  if (!response.ok) {
    throw new Error(`Failed to create session (${response.status})`)
  }
  return response.json()
}

export async function startSimulation(sessionId: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/sessions/${sessionId}/simulate`, { method: 'POST' })
  if (!response.ok) {
    throw new Error(`Failed to start simulation (${response.status})`)
  }
}

export function subscribeToSession(
  sessionId: string,
  onEvent: (event: SessionEvent) => void,
  onError: () => void,
): EventSource {
  const eventSource = new EventSource(`${BASE_URL}/sessions/${sessionId}/stream`)

  eventSource.addEventListener('update', (message) => {
    onEvent(JSON.parse((message as MessageEvent<string>).data) as SessionEvent)
  })
  eventSource.onerror = onError

  return eventSource
}