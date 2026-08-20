import type { SimulationState } from '../state/simulationReducer'

const STATUS_TEXT: Record<Exclude<SimulationState['phase'], 'finished'>, string> = {
  idle: 'Press "Start Simulation" to watch a game.',
  starting: 'Simulation in progress...',
  running: 'Simulation in progress...',
  reconnecting: 'Simulation in progress...',
  failed: 'Simulation failed.',
}

interface StatusBannerProps {
  state: SimulationState
}

function StatusBanner({ state }: StatusBannerProps) {
  const text =
    state.phase === 'finished'
      ? state.outcome === 'DRAW'
        ? "It's a draw!"
        : `Winner: ${state.outcome === 'X_WON' ? 'X' : 'O'}`
      : STATUS_TEXT[state.phase]

  return (
    <p className="status" aria-live="polite">
      {text}
    </p>
  )
}

export default StatusBanner
