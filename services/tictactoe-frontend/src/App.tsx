import { useEffect, useMemo, useRef, useState } from 'react'
import Board from './components/Board'
import MoveLog from './components/MoveLog'
import { ApiError, createSession, getSession, startSimulation, subscribeToSession } from './services/gameApiService'
import { calculateWinner, createEmptyBoard, findChangedIndex, normalizeBoard } from './utils/logic'
import { formatMoveLogEntry } from './utils/moveLog'
import type { BoardState, Player, SessionStatus } from './utils/types'
import './styles/App.css'

/** How long a dropped SSE stream is given to reconnect on its own before the UI gives up and
 * marks the simulation failed. EventSource retries automatically underneath this. */
const RECONNECT_TIMEOUT_MS = 8000
/** Fallback cadence for GET /sessions/{id} while the stream is down. */
const POLL_INTERVAL_MS = 1000

function App() {
  const [sessionId, setSessionId] = useState('')
  const [squares, setSquares] = useState<BoardState>(createEmptyBoard())
  const [status, setStatus] = useState<SessionStatus>('CREATED')
  const [isSimulating, setIsSimulating] = useState(false)
  const [isReconnecting, setIsReconnecting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [logEntries, setLogEntries] = useState<string[]>([])
  const eventSourceRef = useRef<EventSource | null>(null)
  const boardRef = useRef<BoardState>(createEmptyBoard())
  const pollIntervalRef = useRef<number | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)

  const winningLine = useMemo(() => calculateWinner(squares)?.line ?? null, [squares])
  const winnerSymbol: Player | null = useMemo(() => calculateWinner(squares)?.winner ?? null, [squares])

  useEffect(() => {
    return () => cleanupStream()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function stopPolling() {
    if (pollIntervalRef.current !== null) {
      window.clearInterval(pollIntervalRef.current)
      pollIntervalRef.current = null
    }
  }

  function clearReconnectTimeout() {
    if (reconnectTimeoutRef.current !== null) {
      window.clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
  }

  function cleanupStream() {
    eventSourceRef.current?.close()
    eventSourceRef.current = null
    stopPolling()
    clearReconnectTimeout()
  }

  /** Single exit point for every terminal outcome (win/draw/failure/reconnect-timeout) so
   * isSimulating can never be left stuck true because one path forgot to reset it. */
  function finishSimulation(finalStatus: SessionStatus, errorMessage?: string) {
    setStatus(finalStatus)
    setIsSimulating(false)
    setIsReconnecting(false)
    if (errorMessage) {
      setError(errorMessage)
    }
    cleanupStream()
  }

  function startPolling(currentSessionId: string) {
    if (pollIntervalRef.current !== null) {
      return
    }
    pollIntervalRef.current = window.setInterval(() => {
      getSession(currentSessionId)
        .then((session) => {
          boardRef.current = normalizeBoard(session.board)
          setSquares(boardRef.current)
          if (session.status !== 'IN_PROGRESS' && session.status !== 'CREATED') {
            finishSimulation(session.status, session.errorMessage ?? undefined)
          }
        })
        .catch(() => {
          // Transient — keep polling until the reconnect timeout below gives up.
        })
    }, POLL_INTERVAL_MS)
  }

  /** EventSource.onerror fires on transient disconnects too, and the browser retries on its
   * own — closing on the first error would permanently disable that. Instead: show
   * "reconnecting", poll as a fallback, and only give up after RECONNECT_TIMEOUT_MS. */
  function handleStreamError(currentSessionId: string) {
    setIsReconnecting(true)
    startPolling(currentSessionId)
    if (reconnectTimeoutRef.current === null) {
      reconnectTimeoutRef.current = window.setTimeout(() => {
        finishSimulation('FAILED', 'Lost connection to the session stream.')
      }, RECONNECT_TIMEOUT_MS)
    }
  }

  function handleStreamOpen() {
    setIsReconnecting(false)
    stopPolling()
    clearReconnectTimeout()
  }

  async function handleStartSimulation() {
    setError(null)
    setStatus('IN_PROGRESS')
    setIsSimulating(true)
    setIsReconnecting(false)
    setLogEntries([])
    boardRef.current = createEmptyBoard()
    setSquares(boardRef.current)

    let currentSessionId = sessionId
    try {
      if (!currentSessionId) {
        const session = await createSession()
        currentSessionId = session.sessionId
        setSessionId(currentSessionId)
      }

      cleanupStream()
      eventSourceRef.current = subscribeToSession(
        currentSessionId,
        (event) => {
          if (event.type === 'FAILED') {
            finishSimulation('FAILED', event.errorMessage ?? `Simulation failed (${event.errorCode ?? 'unknown error'}).`)
            return
          }

          const normalizedBoard = normalizeBoard(event.board ?? [])
          const changedIndex = findChangedIndex(boardRef.current, normalizedBoard)
          boardRef.current = normalizedBoard
          setSquares(normalizedBoard)

          const stepStatus = event.stepStatus
          if (stepStatus) {
            const player = changedIndex !== null ? (normalizedBoard[changedIndex] as Player) : null
            setLogEntries((entries) => [
              ...entries,
              formatMoveLogEntry(new Date(), stepStatus, player, changedIndex !== null ? changedIndex + 1 : null),
            ])
          }

          if (event.type === 'COMPLETED') {
            finishSimulation(event.winner ?? 'DRAW')
          } else {
            setStatus('IN_PROGRESS')
          }
        },
        () => handleStreamError(currentSessionId),
        handleStreamOpen,
      )

      await startSimulation(currentSessionId)
    } catch (err) {
      const message = err instanceof ApiError || err instanceof Error ? err.message : 'Failed to start simulation.'
      finishSimulation('CREATED', message)
    }
  }

  const statusText =
    status === 'X_WON' || status === 'O_WON'
      ? `Winner: ${winnerSymbol ?? '?'}`
      : status === 'DRAW'
        ? "It's a draw!"
        : status === 'FAILED'
          ? 'Simulation failed.'
          : status === 'CANCELLED'
            ? 'Simulation cancelled.'
            : status === 'IN_PROGRESS'
              ? 'Simulation in progress...'
              : 'Press "Start Simulation" to watch a game.'

  return (
    <main className="app">
      <div className="game">
        <h1>Tic Tac Toe</h1>
        <p className="status">{statusText}</p>
        {isReconnecting && <p className="reconnecting">Reconnecting…</p>}
        {error && <p className="error">{error}</p>}
        <Board squares={squares} winningLine={winningLine} />
        <button className="start" onClick={handleStartSimulation} disabled={isSimulating}>
          {isSimulating ? 'Simulating...' : 'Start Simulation'}
        </button>
      </div>
      <MoveLog entries={logEntries} />
    </main>
  )
}

export default App
