import { useCallback, useEffect, useReducer, useRef, useState } from 'react'
import { ApiError, createSession, startSimulation } from '../services/gameApiService'
import { createInitialState, simulationReducer } from '../state/simulationReducer'
import { normalizeBoard } from '../utils/logic'
import type { SessionEvent, SessionResponse } from '../utils/types'
import { useSessionPolling } from './useSessionPolling'
import { useSseSubscription } from './useSseSubscription'

/** How long a dropped SSE stream is given to reconnect on its own before the UI gives up and
 * marks the simulation failed. EventSource retries automatically underneath this. */
const RECONNECT_TIMEOUT_MS = 8000

/** Composes the SSE subscription and the polling fallback around the simulation reducer and
 * exposes the one surface App needs: current state, whether a run is in flight, and how to
 * start one. */
export function useSimulation() {
  const [state, dispatch] = useReducer(simulationReducer, undefined, createInitialState)
  const [sessionId, setSessionId] = useState<string | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)

  const isSimulating = state.phase === 'starting' || state.phase === 'running' || state.phase === 'reconnecting'

  const clearReconnectTimeout = useCallback(() => {
    if (reconnectTimeoutRef.current !== null) {
      window.clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
  }, [])

  const handleEvent = useCallback((event: SessionEvent) => {
    if (event.type === 'FAILED') {
      dispatch({
        type: 'FAILED',
        message: event.errorMessage ?? `Simulation failed (${event.errorCode ?? 'unknown error'}).`,
      })
      return
    }
    dispatch({ type: 'MOVE_APPLIED', board: normalizeBoard(event.board ?? []), stepStatus: event.stepStatus })
    if (event.type === 'COMPLETED') {
      dispatch({ type: 'COMPLETED', outcome: event.winner ?? 'DRAW' })
    }
  }, [])

  /** onerror fires on transient disconnects too, and the browser retries on its own — closing
   * on the first error would permanently disable that. Instead: mark reconnecting, poll as a
   * fallback, and only give up after RECONNECT_TIMEOUT_MS. */
  const handleStreamError = useCallback(() => {
    dispatch({ type: 'STREAM_LOST' })
    if (reconnectTimeoutRef.current === null) {
      reconnectTimeoutRef.current = window.setTimeout(() => {
        dispatch({ type: 'FAILED', message: 'Lost connection to the session stream.' })
      }, RECONNECT_TIMEOUT_MS)
    }
  }, [])

  const handleStreamOpen = useCallback(() => {
    clearReconnectTimeout()
    dispatch({ type: 'STREAM_RESTORED' })
  }, [clearReconnectTimeout])

  useSseSubscription(sessionId, { onEvent: handleEvent, onError: handleStreamError, onOpen: handleStreamOpen })

  const handleSnapshot = useCallback((session: SessionResponse) => {
    dispatch({ type: 'SNAPSHOT', session })
  }, [])

  useSessionPolling(sessionId, state.phase === 'reconnecting', handleSnapshot)

  useEffect(() => clearReconnectTimeout, [clearReconnectTimeout])

  // A session is single-use — the session state machine has no path back out of a terminal
  // status, and the engine's game for it is finished. Reusing a previous run's sessionId here
  // used to leave the UI stuck: the reused session's SSE sequence/backlog resets server-side on
  // completion, but the browser's EventSource keeps the old (higher) Last-Event-ID, so the
  // replay filter silently drops every event of the new run and the button never re-enables.
  // Always mint a fresh session per run instead.
  const start = useCallback(async () => {
    clearReconnectTimeout()
    dispatch({ type: 'START' })
    try {
      const session = await createSession()
      setSessionId(session.sessionId)
      await startSimulation(session.sessionId)
    } catch (err) {
      const message = err instanceof ApiError || err instanceof Error ? err.message : 'Failed to start simulation.'
      dispatch({ type: 'FAILED', message })
    }
  }, [clearReconnectTimeout])

  return { state, isSimulating, start }
}
