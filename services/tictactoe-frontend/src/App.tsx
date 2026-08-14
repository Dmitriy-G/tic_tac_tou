import { useMemo } from 'react'
import Board from './components/Board'
import MoveLog from './components/MoveLog'
import StatusBanner from './components/StatusBanner'
import { useSimulation } from './hooks/useSimulation'
import { calculateWinner } from './utils/logic'
import './styles/App.css'

function App() {
  const { state, isSimulating, start } = useSimulation()
  const winningLine = useMemo(() => calculateWinner(state.board)?.line ?? null, [state.board])

  return (
    <main className="app">
      <div className="game">
        <h1>Tic Tac Toe</h1>
        <StatusBanner state={state} />
        {state.phase === 'reconnecting' && (
          <p className="reconnecting" aria-live="polite">
            Reconnecting…
          </p>
        )}
        {state.phase === 'failed' && (
          <p className="error" aria-live="polite">
            {state.message}
          </p>
        )}
        <Board squares={state.board} winningLine={winningLine} />
        <button className="start" onClick={start} disabled={isSimulating}>
          {isSimulating ? 'Simulating...' : 'Start Simulation'}
        </button>
      </div>
      <MoveLog entries={state.moves} />
    </main>
  )
}

export default App
