import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ErrorBoundary from './ErrorBoundary'

function Bomb(): never {
  throw new Error('boom')
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    // React logs the caught error to console.error even though the boundary handles it.
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders a fallback instead of white-screening when a child throws', () => {
    render(
      <ErrorBoundary>
        <Bomb />
      </ErrorBoundary>,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong. Please reload the page.')
  })
})
