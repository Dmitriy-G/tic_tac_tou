import type { BoardState, Player, WinResult } from './types'

const BOARD_SIZE = 9

const WINNING_LINES: number[][] = [
  [0, 1, 2],
  [3, 4, 5],
  [6, 7, 8],
  [0, 3, 6],
  [1, 4, 7],
  [2, 5, 8],
  [0, 4, 8],
  [2, 4, 6],
]

export function createEmptyBoard(): BoardState {
  return Array<null>(BOARD_SIZE).fill(null)
}

export function normalizeBoard(board: string[]): BoardState {
  return board.map((cell) => (cell === 'X' || cell === 'O' ? cell : null))
}

/** The UI's only remaining rule computation. The engine is the authority on the outcome — this
 * derives the winning triple for the highlight because the API doesn't return it yet
 * (see GameServiceImpl DEFERRED-7: GameResponse exposes no winningLine). Delete this the day the
 * backend sends it. */
export function calculateWinner(squares: BoardState): WinResult | null {
  for (const line of WINNING_LINES) {
    const [a, b, c] = line
    const value = squares[a]
    if (value && value === squares[b] && value === squares[c]) {
      return { winner: value as Player, line }
    }
  }
  return null
}

export function findChangedIndex(previous: BoardState, next: BoardState): number | null {
  for (let i = 0; i < next.length; i++) {
    if (next[i] !== null && next[i] !== previous[i]) {
      return i
    }
  }
  return null
}