import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import StatusBanner from './StatusBanner'
import { createEmptyBoard } from '../utils/logic'

describe('StatusBanner', () => {
  it.each<['idle' | 'starting' | 'running' | 'reconnecting', string]>([
    ['idle', 'Press "Start Simulation" to watch a game.'],
    ['starting', 'Simulation in progress...'],
    ['running', 'Simulation in progress...'],
    ['reconnecting', 'Simulation in progress...'],
  ])('renders the right text for phase %s', (phase, expectedText) => {
    render(<StatusBanner state={{ phase, board: createEmptyBoard(), moves: [] }} />)

    expect(screen.getByText(expectedText)).toBeInTheDocument()
  })

  it('renders the failure message when failed', () => {
    render(<StatusBanner state={{ phase: 'failed', board: createEmptyBoard(), moves: [], message: 'boom' }} />)

    expect(screen.getByText('Simulation failed.')).toBeInTheDocument()
  })

  it('renders a draw message when finished in a draw', () => {
    render(<StatusBanner state={{ phase: 'finished', board: createEmptyBoard(), moves: [], outcome: 'DRAW' }} />)

    expect(screen.getByText("It's a draw!")).toBeInTheDocument()
  })

  it('renders the winner when finished with a win', () => {
    render(<StatusBanner state={{ phase: 'finished', board: createEmptyBoard(), moves: [], outcome: 'X_WON' }} />)

    expect(screen.getByText('Winner: X')).toBeInTheDocument()
  })

  it('has aria-live="polite"', () => {
    render(<StatusBanner state={{ phase: 'idle', board: createEmptyBoard(), moves: [] }} />)

    expect(screen.getByText(/Start Simulation/)).toHaveAttribute('aria-live', 'polite')
  })
})
