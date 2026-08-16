import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import MoveLog from './MoveLog'
import type { MoveHistoryEntry } from '../utils/types'

describe('MoveLog', () => {
  it('renders one item per entry', () => {
    const entries: MoveHistoryEntry[] = [
      { moveNumber: 1, symbol: 'X', position: 0, stepStatus: 'CORRECT_STEP' },
      { moveNumber: 2, symbol: 'O', position: 4, stepStatus: 'CORRECT_STEP' },
    ]

    render(<MoveLog entries={entries} />)

    expect(screen.getAllByRole('listitem')).toHaveLength(2)
  })

  it('renders the empty state when there are none', () => {
    render(<MoveLog entries={[]} />)

    expect(screen.getByText('No moves yet.')).toBeInTheDocument()
  })
})
