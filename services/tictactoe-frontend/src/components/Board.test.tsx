import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Board from './Board'
import { createEmptyBoard } from '../utils/logic'

describe('Board', () => {
  it('renders 9 cells and applies square--winning only to the winning triple', () => {
    const squares = createEmptyBoard()
    squares[0] = 'X'
    squares[1] = 'X'
    squares[2] = 'X'

    render(<Board squares={squares} winningLine={[0, 1, 2]} />)

    const cells = screen.getAllByRole('gridcell')
    expect(cells).toHaveLength(9)
    const winning = cells.filter((cell) => cell.className.includes('square--winning'))
    expect(winning.map((cell) => cell.textContent)).toEqual(['X', 'X', 'X'])
  })

  it('has one role="row" per three cells', () => {
    render(<Board squares={createEmptyBoard()} winningLine={null} />)

    expect(screen.getAllByRole('row')).toHaveLength(3)
  })
})
