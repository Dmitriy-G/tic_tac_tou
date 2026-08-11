import type { BoardState, Player, WinResult } from './types'

export const BOARD_SIZE = 9

export const WINNING_LINES: number[][] = [
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

export function isBoardFull(squares: BoardState): boolean {
  return squares.every((square) => square !== null)
}

export function findChangedIndex(previous: BoardState, next: BoardState): number | null {
  for (let i = 0; i < next.length; i++) {
    if (next[i] !== null && next[i] !== previous[i]) {
      return i
    }
  }
  return null
}