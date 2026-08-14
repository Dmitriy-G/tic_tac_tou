import { describe, expect, it } from 'vitest'
import { createInitialState, simulationReducer, type SimulationState } from './simulationReducer'
import type { SessionResponse } from '../utils/types'

function runningState(board: string[] = ['X', '.', '.', '.', '.', '.', '.', '.', '.']): SimulationState {
  return simulationReducer(
    simulationReducer(createInitialState(), { type: 'START' }),
    { type: 'MOVE_APPLIED', board: board.map((c) => (c === 'X' || c === 'O' ? c : null)), stepStatus: 'CORRECT_STEP' },
  )
}

function session(overrides: Partial<SessionResponse>): SessionResponse {
  return {
    sessionId: 'session-1',
    status: 'IN_PROGRESS',
    board: ['.', '.', '.', '.', '.', '.', '.', '.', '.'],
    moves: [],
    winner: null,
    errorCode: null,
    errorMessage: null,
    ...overrides,
  }
}

describe('simulationReducer', () => {
  it('starts idle with an empty board and no moves', () => {
    const state = createInitialState()
    expect(state).toEqual({ phase: 'idle', board: Array(9).fill(null), moves: [] })
  })

  it('START resets to starting with a fresh empty board, even from a running state', () => {
    const state = simulationReducer(runningState(), { type: 'START' })
    expect(state).toEqual({ phase: 'starting', board: Array(9).fill(null), moves: [] })
  })

  it('MOVE_APPLIED appends exactly one entry and moves to running', () => {
    const started = simulationReducer(createInitialState(), { type: 'START' })
    const state = simulationReducer(started, {
      type: 'MOVE_APPLIED',
      board: [null, null, null, null, 'X', null, null, null, null],
      stepStatus: 'CORRECT_STEP',
    })
    expect(state.phase).toBe('running')
    expect(state.moves).toEqual([{ moveNumber: 1, symbol: 'X', position: 4, stepStatus: 'CORRECT_STEP' }])
  })

  it('MOVE_APPLIED does not append when the board did not change', () => {
    const started = simulationReducer(createInitialState(), { type: 'START' })
    const state = simulationReducer(started, {
      type: 'MOVE_APPLIED',
      board: Array(9).fill(null),
      stepStatus: 'CORRECT_STEP',
    })
    expect(state.moves).toEqual([])
  })

  it('MOVE_APPLIED does not append when stepStatus is null, but still updates the board', () => {
    const started = simulationReducer(createInitialState(), { type: 'START' })
    const board = [null, null, null, null, 'X', null, null, null, null] as const
    const state = simulationReducer(started, { type: 'MOVE_APPLIED', board: [...board], stepStatus: null })
    expect(state.moves).toEqual([])
    expect(state.board).toEqual(board)
  })

  it('a second MOVE_APPLIED numbers the entry from the existing move count', () => {
    const first = runningState(['X', '.', '.', '.', '.', '.', '.', '.', '.'])
    const state = simulationReducer(first, {
      type: 'MOVE_APPLIED',
      board: ['X', 'O', null, null, null, null, null, null, null],
      stepStatus: 'CORRECT_STEP',
    })
    expect(state.moves).toHaveLength(2)
    expect(state.moves[1]).toEqual({ moveNumber: 2, symbol: 'O', position: 1, stepStatus: 'CORRECT_STEP' })
  })

  it('STREAM_LOST moves to reconnecting, STREAM_RESTORED moves back to running', () => {
    const lost = simulationReducer(runningState(), { type: 'STREAM_LOST' })
    expect(lost.phase).toBe('reconnecting')

    const restored = simulationReducer(lost, { type: 'STREAM_RESTORED' })
    expect(restored.phase).toBe('running')
  })

  it('COMPLETED moves to finished carrying the outcome', () => {
    const state = simulationReducer(runningState(), { type: 'COMPLETED', outcome: 'X_WON' })
    expect(state).toMatchObject({ phase: 'finished', outcome: 'X_WON' })
  })

  it('FAILED moves to failed carrying the message', () => {
    const state = simulationReducer(runningState(), { type: 'FAILED', message: 'boom' })
    expect(state).toMatchObject({ phase: 'failed', message: 'boom' })
  })

  it('terminal phases are absorbing: SNAPSHOT after FAILED is a no-op', () => {
    const failed = simulationReducer(runningState(), { type: 'FAILED', message: 'boom' })
    const afterSnapshot = simulationReducer(failed, { type: 'SNAPSHOT', session: session({ status: 'IN_PROGRESS' }) })
    expect(afterSnapshot).toBe(failed)
  })

  it('terminal phases are absorbing: MOVE_APPLIED after COMPLETED is a no-op', () => {
    const finished = simulationReducer(runningState(), { type: 'COMPLETED', outcome: 'DRAW' })
    const afterMove = simulationReducer(finished, {
      type: 'MOVE_APPLIED',
      board: Array(9).fill('X'),
      stepStatus: 'CORRECT_STEP',
    })
    expect(afterMove).toBe(finished)
  })

  it('a terminal phase only yields to START', () => {
    const finished = simulationReducer(runningState(), { type: 'COMPLETED', outcome: 'DRAW' })
    const restarted = simulationReducer(finished, { type: 'START' })
    expect(restarted).toEqual({ phase: 'starting', board: Array(9).fill(null), moves: [] })
  })

  describe('SNAPSHOT', () => {
    it('replaces board and moves from the session while still in progress', () => {
      const moves = [{ moveNumber: 1, symbol: 'X' as const, position: 4, stepStatus: 'CORRECT_STEP' as const }]
      const state = simulationReducer(runningState(), {
        type: 'SNAPSHOT',
        session: session({ status: 'IN_PROGRESS', board: ['.', '.', '.', '.', 'X', '.', '.', '.', '.'], moves }),
      })
      expect(state.phase).toBe('running')
      expect(state.moves).toEqual(moves)
      expect(state.board[4]).toBe('X')
    })

    it('a terminal session status transitions straight to finished', () => {
      const state = simulationReducer(runningState(), {
        type: 'SNAPSHOT',
        session: session({ status: 'O_WON' }),
      })
      expect(state).toMatchObject({ phase: 'finished', outcome: 'O_WON' })
    })

    it('a FAILED session status transitions to failed with the session error message', () => {
      const state = simulationReducer(runningState(), {
        type: 'SNAPSHOT',
        session: session({ status: 'FAILED', errorMessage: 'engine unreachable' }),
      })
      expect(state).toMatchObject({ phase: 'failed', message: 'engine unreachable' })
    })
  })
})
