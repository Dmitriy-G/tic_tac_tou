import { useEffect } from 'react'
import { subscribeToSession } from '../services/gameApiService'
import type { SessionEvent } from '../utils/types'

interface SseHandlers {
  onEvent: (event: SessionEvent) => void
  onError: () => void
  onOpen: () => void
}

/** Owns the EventSource lifecycle only: opens on a session id, dispatches parsed events through
 * the given handlers, and closes on unmount or id change. Knows nothing about polling or
 * simulation phases — that composition lives in useSimulation. */
export function useSseSubscription(sessionId: string | null, handlers: SseHandlers): void {
  const { onEvent, onError, onOpen } = handlers

  useEffect(() => {
    if (!sessionId) {
      return
    }
    const eventSource = subscribeToSession(sessionId, onEvent, onError, onOpen)
    return () => eventSource.close()
  }, [sessionId, onEvent, onError, onOpen])
}
