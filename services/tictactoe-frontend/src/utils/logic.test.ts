import { describe, expect, it } from 'vitest'
import { WINNING_LINES, calculateWinner, createEmptyBoard, findChangedIndex, normalizeBoard } from './logic'
import type { BoardState } from './types'

function boardWithLine(line: number[], symbol: 'X' | 'O'): BoardState {
  const board = createEmptyBoard()
  for (const index of line) {
    board[index] = symbol
  }
  return board
}

describe('createEmptyBoard', () => {
  it('returns 9 empty cells', () => {
    expect(createEmptyBoard()).toEqual(Array(9).fill(null))
  })
})

describe('calculateWinner', () => {
  it.each(WINNING_LINES)('detects a win on cells %i,%i,%i', (...line) => {
    const board = boardWithLine(line, 'X')
    expect(calculateWinner(board)).toEqual({ winner: 'X', line })
  })

  it('returns null when there is no winner', () => {
    const board: BoardState = ['X', 'O', 'X', 'X', 'O', 'O', 'O', 'X', 'X']
    expect(calculateWinner(board)).toBeNull()
  })

  it('returns null on an empty board', () => {
    expect(calculateWinner(createEmptyBoard())).toBeNull()
  })
})

describe('normalizeBoard', () => {
  it('maps X and O through unchanged', () => {
    expect(normalizeBoard(['X', 'O'])).toEqual(['X', 'O'])
  })

  it('maps anything else — including garbage — to null', () => {
    expect(normalizeBoard(['_', '', 'x', 'o', 'garbage', '.'])).toEqual([null, null, null, null, null, null])
  })
})

describe('findChangedIndex', () => {
  it('returns null when nothing changed', () => {
    const board = boardWithLine([0], 'X')
    expect(findChangedIndex(board, [...board])).toBeNull()
  })

  it('returns null on two empty boards', () => {
    expect(findChangedIndex(createEmptyBoard(), createEmptyBoard())).toBeNull()
  })

  it('finds the single cell that changed from empty to occupied', () => {
    const previous = createEmptyBoard()
    const next = boardWithLine([4], 'X')
    expect(findChangedIndex(previous, next)).toBe(4)
  })
})
