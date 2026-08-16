import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Square from './Square'

describe('Square', () => {
  it('renders the value and an aria-label for an occupied cell', () => {
    render(<Square value="X" isWinning={false} />)

    const cell = screen.getByRole('gridcell')
    expect(cell).toHaveTextContent('X')
    expect(cell).toHaveAttribute('aria-label', 'Square: X')
  })

  it('renders an empty-square aria-label when unoccupied', () => {
    render(<Square value={null} isWinning={false} />)

    expect(screen.getByRole('gridcell')).toHaveAttribute('aria-label', 'Empty square')
  })

  it('applies square--winning only when isWinning is true', () => {
    render(<Square value="O" isWinning />)

    expect(screen.getByRole('gridcell').className).toContain('square--winning')
  })
})
