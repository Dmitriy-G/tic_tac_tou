import { useEffect, useMemo, useRef, useState } from 'react'
import Board from './components/Board'
import { createSession, startSimulation, subscribeToSession } from './api/sessionApi'
import { calculateWinner, createEmptyBoard } from './game/logic'
import type { BoardState, Player, SessionStatus } from './game/types'
import './App.css'

function App() {
  const [sessionId, setSessionId] = useState('')
  const [squares, setSquares] = useState<BoardState>(createEmptyBoard())
  const [status, setStatus] = useState<SessionStatus>('CREATED')
  const [winner, setWinner] = useState<Player | null>(null)
  const [isSimulating, setIsSimulating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const eventSourceRef = useRef<EventSource | null>(null)

  const winningLine = useMemo(() => calculateWinner(squares)?.line ?? null, [squares])

  useEffect(() => {
    return () => eventSourceRef.current?.close()
  }, [])

  async function handleStartSimulation() {
    setError(null)
    setWinner(null)
    setIsSimulating(true)

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
          setSquares(event.board)
          setStatus(event.status)
          setWinner(event.winner)
          if (event.status === 'WIN' || event.status === 'DRAW') {
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
    status === 'WIN'
      ? `Winner: ${winner ?? '?'}`
      : status === 'DRAW'
        ? "It's a draw!"
        : status === 'IN_PROGRESS'
          ? 'Simulation in progress…'
          : 'Press "Start Simulation" to watch a game.'

  return (
    <main className="app">
      <h1>Tic Tac Toe</h1>
      <p className="status">{statusText}</p>
      {error && <p className="error">{error}</p>}
      <Board squares={squares} winningLine={winningLine} gameOver={false}
             onSquareClick={function (index: number): void {
               throw new Error("Function not implemented.")
             }} />
      <button className="start" onClick={handleStartSimulation} disabled={isSimulating}>
        {isSimulating ? 'Simulating…' : 'Start Simulation'}
      </button>
    </main>
  )
}

export default App