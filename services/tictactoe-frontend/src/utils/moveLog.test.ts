import { describe, expect, it } from 'vitest'
import { formatMoveLogEntry } from './moveLog'
import type { MoveHistoryEntry } from './types'

describe('formatMoveLogEntry', () => {
  it('formats a correct step with its 1-based move number, symbol, and cell', () => {
    const entry: MoveHistoryEntry = { moveNumber: 1, symbol: 'X', position: 4, stepStatus: 'CORRECT_STEP' }
    expect(formatMoveLogEntry(entry)).toBe('Move 1: X was set to 5 cell. CORRECT_STEP')
  })

  it('formats a later move for the other symbol at a different cell', () => {
    const entry: MoveHistoryEntry = { moveNumber: 6, symbol: 'O', position: 0, stepStatus: 'GAME_FINISHED' }
    expect(formatMoveLogEntry(entry)).toBe('Move 6: O was set to 1 cell. GAME_FINISHED')
  })
})
