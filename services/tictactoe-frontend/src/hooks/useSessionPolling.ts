import { useEffect } from 'react'
import { getSession } from '../services/gameApiService'
import type { SessionResponse } from '../utils/types'

/** Fallback cadence for GET /sessions/{id} while the SSE stream is down. */
const POLL_INTERVAL_MS = 1000

/** setInterval over GET /sessions/{id} while enabled. Knows nothing about SSE — the caller
 * decides when polling should run (here: only while reconnecting). */
export function useSessionPolling(
  sessionId: string | null,
  enabled: boolean,
  onSnapshot: (session: SessionResponse) => void,
): void {
  useEffect(() => {
    if (!enabled || !sessionId) {
      return
    }
    const intervalId = window.setInterval(() => {
      getSession(sessionId)
        .then(onSnapshot)
        .catch(() => {
          // Transient — keep polling until the caller's reconnect timeout gives up.
        })
    }, POLL_INTERVAL_MS)
    return () => window.clearInterval(intervalId)
  }, [sessionId, enabled, onSnapshot])
}
