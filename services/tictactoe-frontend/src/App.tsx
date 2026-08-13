import { useEffect, useMemo, useRef, useState } from 'react'
import Board from './components/Board'
import MoveLog from './components/MoveLog'
import { createSession, startSimulation, subscribeToSession } from './services/gameApiService'
import { calculateWinner, createEmptyBoard, findChangedIndex } from './utils/logic'
import { formatMoveLogEntry } from './utils/moveLog'
import type { BoardState, Player, SessionStatus } from './utils/types'
import './styles/App.css'

function App() {
  const [sessionId, setSessionId] = useState('')
  const [squares, setSquares] = useState<BoardState>(createEmptyBoard())
  const [status, setStatus] = useState<SessionStatus>('CREATED')
  const [isSimulating, setIsSimulating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [logEntries, setLogEntries] = useState<string[]>([])
  const eventSourceRef = useRef<EventSource | null>(null)
  const boardRef = useRef<BoardState>(createEmptyBoard())

  const winningLine = useMemo(() => calculateWinner(squares)?.line ?? null, [squares])
  const winnerSymbol: Player | null = useMemo(() => calculateWinner(squares)?.winner ?? null, [squares])

  useEffect(() => {
    return () => eventSourceRef.current?.close()
  }, [])

  async function handleStartSimulation() {
    setError(null)
    setStatus('IN_PROGRESS')
    setIsSimulating(true)
    setLogEntries([])
    boardRef.current = createEmptyBoard()
    setSquares(boardRef.current)

    try {
      let currentSessionId = sessionId
      if (!currentSessionId) {
        const session = await createSession()
        currentSessionId = session.sessionId
        setSessionId(currentSessionId)
      }

      eventSourceRef.current?.close()
      eventSourceRef.current = subscribeToSession(
        currentSessionId,
        (event) => {
          const changedIndex = findChangedIndex(boardRef.current, event.board)
          boardRef.current = event.board

          setSquares(event.board)
          setStatus(event.winner ?? 'IN_PROGRESS')

          if (changedIndex !== null) {
            const player = event.board[changedIndex] as Player
            setLogEntries((entries) => [
              ...entries,
              formatMoveLogEntry(new Date(), player, changedIndex + 1, event.stepStatus),
            ])
          }

          if (event.winner !== null) {
            setIsSimulating(false)
            eventSourceRef.current?.close()
          }
        },
        () => {
          setError('Lost connection to the simulation stream.')
          setIsSimulating(false)
          eventSourceRef.current?.close()
        },
      )

      await startSimulation(currentSessionId)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start simulation.')
      setIsSimulating(false)
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