import { renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useSessionPolling } from './useSessionPolling'
import * as gameApiService from '../services/gameApiService'
import type { SessionResponse } from '../utils/types'

vi.mock('../services/gameApiService')

const SNAPSHOT: SessionResponse = {
  sessionId: 'session-1',
  status: 'IN_PROGRESS',
  board: Array(9).fill('.'),
  moves: [],
  winner: null,
  errorCode: null,
  errorMessage: null,
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  vi.mocked(gameApiService.getSession).mockResolvedValue(SNAPSHOT)
})

afterEach(() => {
  vi.useRealTimers()
})

describe('useSessionPolling', () => {
  it('polls GET /sessions/{id} on the configured interval while enabled', async () => {
    const onSnapshot = vi.fn()
    renderHook(() => useSessionPolling('session-1', true, onSnapshot))

    await vi.advanceTimersByTimeAsync(1000)

    expect(gameApiService.getSession).toHaveBeenCalledWith('session-1')
  })

  it('does not poll while disabled', async () => {
    renderHook(() => useSessionPolling('session-1', false, vi.fn()))

    await vi.advanceTimersByTimeAsync(5000)

    expect(gameApiService.getSession).not.toHaveBeenCalled()
  })

  it('stops polling on unmount', async () => {
    const { unmount } = renderHook(() => useSessionPolling('session-1', true, vi.fn()))
    await vi.advanceTimersByTimeAsync(1000)
    vi.mocked(gameApiService.getSession).mockClear()

    unmount()
    await vi.advanceTimersByTimeAsync(5000)

    expect(gameApiService.getSession).not.toHaveBeenCalled()
  })
})
