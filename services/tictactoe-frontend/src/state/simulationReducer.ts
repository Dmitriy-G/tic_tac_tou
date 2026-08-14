import { createEmptyBoard, findChangedIndex, normalizeBoard } from '../utils/logic'
import type { BoardState, GameOutcome, MoveHistoryEntry, Player, SessionResponse, StepStatus } from '../utils/types'

export type SimulationState =
  | { phase: 'idle'; board: BoardState; moves: MoveHistoryEntry[] }
  | { phase: 'starting'; board: BoardState; moves: MoveHistoryEntry[] }
  | { phase: 'running'; board: BoardState; moves: MoveHistoryEntry[] }
  | { phase: 'reconnecting'; board: BoardState; moves: MoveHistoryEntry[] }
  | { phase: 'finished'; board: BoardState; moves: MoveHistoryEntry[]; outcome: GameOutcome }
  | { phase: 'failed'; board: BoardState; moves: MoveHistoryEntry[]; message: string }

export type SimulationAction =
  | { type: 'START' }
  | { type: 'MOVE_APPLIED'; board: BoardState; stepStatus: StepStatus | null }
  | { type: 'STREAM_LOST' }
  | { type: 'STREAM_RESTORED' }
  | { type: 'SNAPSHOT'; session: SessionResponse }
  | { type: 'COMPLETED'; outcome: GameOutcome }
  | { type: 'FAILED'; message: string }

export function createInitialState(): SimulationState {
  return { phase: 'idle', board: createEmptyBoard(), moves: [] }
}

function isTerminal(state: SimulationState): boolean {
  return state.phase === 'finished' || state.phase === 'failed'
}

export function simulationReducer(state: SimulationState, action: SimulationAction): SimulationState {
  // Terminal phases are absorbing: once a simulation has finished or failed, nothing but a fresh
  // START can move it out of that phase. This is what stops a late SNAPSHOT — a poll response
  // that was already in flight when a FAILED/COMPLETED event arrived — from reviving a session
  // the UI already reported as done (the reconnect-timeout race the old flag-based state allowed).
  if (isTerminal(state) && action.type !== 'START') {
    return state
  }

  switch (action.type) {
    case 'START':
      return { phase: 'starting', board: createEmptyBoard(), moves: [] }

    case 'MOVE_APPLIED':
      return { phase: 'running', board: action.board, moves: appendMove(state.moves, state.board, action) }

    case 'STREAM_LOST':
      return { ...state, phase: 'reconnecting' }

    case 'STREAM_RESTORED':
      return { ...state, phase: 'running' }

    case 'SNAPSHOT':
      return applySnapshot(state, action.session)

    case 'COMPLETED':
      return { phase: 'finished', board: state.board, moves: state.moves, outcome: action.outcome }

    case 'FAILED':
      return { phase: 'failed', board: state.board, moves: state.moves, message: action.message }
  }
}

/** The previous board comes from reducer state rather than a ref kept alongside it, so there is
 * no separate copy that can disagree with what's on screen. */
function appendMove(
  moves: MoveHistoryEntry[],
  previousBoard: BoardState,
  action: { board: BoardState; stepStatus: StepStatus | null },
): MoveHistoryEntry[] {
  const changedIndex = findChangedIndex(previousBoard, action.board)
  if (action.stepStatus === null || changedIndex === null) {
    return moves
  }
  const entry: MoveHistoryEntry = {
    moveNumber: moves.length + 1,
    symbol: action.board[changedIndex] as Player,
    position: changedIndex,
    stepStatus: action.stepStatus,
  }
  return [...moves, entry]
}

function applySnapshot(state: SimulationState, session: SessionResponse): SimulationState {
  const board = normalizeBoard(session.board)
  switch (session.status) {
    case 'CREATED':
    case 'IN_PROGRESS':
      return { ...state, board, moves: session.moves }
    case 'FAILED':
      return { phase: 'failed', board, moves: session.moves, message: session.errorMessage ?? 'Simulation failed.' }
    default:
      return { phase: 'finished', board, moves: session.moves, outcome: session.status }
  }
}
