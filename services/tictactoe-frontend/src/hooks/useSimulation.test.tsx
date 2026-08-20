import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useSimulation } from './useSimulation'
import { FakeEventSource } from '../test/fakeEventSource'
import * as gameApiService from '../services/gameApiService'
import type { SessionResponse } from '../utils/types'

vi.mock('../services/gameApiService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../services/gameApiService')>()
  return {
    ...actual,
    createSession: vi.fn(),
    startSimulation: vi.fn(),
    getSession: vi.fn(),
  }
})

const IN_PROGRESS_SNAPSHOT: SessionResponse = {
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
  FakeEventSource.instances = []
  vi.stubGlobal('EventSource', FakeEventSource)
  vi.mocked(gameApiService.createSession).mockResolvedValue({
    sessionId: 'session-1',
    status: 'CREATED',
    board: Array(9).fill('.'),
    moves: [],
    winner: null,
    errorCode: null,
    errorMessage: null,
  })
  vi.mocked(gameApiService.startSimulation).mockResolvedValue(undefined)
  vi.mocked(gameApiService.getSession).mockResolvedValue(IN_PROGRESS_SNAPSHOT)
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

async function startAndOpenStream() {
  const { result } = renderHook(() => useSimulation())
  await act(async () => {
    await result.current.start()
  })
  return { result, instance: FakeEventSource.instances[0] }
}

describe('useSimulation', () => {
  it('switches to polling when the stream errors', async () => {
    const { result, instance } = await startAndOpenStream()

    act(() => instance.onerror?.())

    expect(result.current.state.phase).toBe('reconnecting')
  })

  it('polls the session while reconnecting', async () => {
    const { instance } = await startAndOpenStream()

    act(() => instance.onerror?.())
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000)
    })

    expect(gameApiService.getSession).toHaveBeenCalledWith('session-1')
  })

  it('stops polling when the stream recovers', async () => {
    const { result, instance } = await startAndOpenStream()
    act(() => instance.onerror?.())
    expect(result.current.state.phase).toBe('reconnecting')

    act(() => instance.onopen?.())
    expect(result.current.state.phase).toBe('running')

    vi.mocked(gameApiService.getSession).mockClear()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000)
    })
    expect(gameApiService.getSession).not.toHaveBeenCalled()
  })

  it('a terminal event stops both the reconnect timeout and polling', async () => {
    const { result, instance } = await startAndOpenStream()

    act(() => {
      instance.emit('update', {
        sessionId: 'session-1',
        type: 'COMPLETED',
        board: Array(9).fill('X'),
        stepStatus: 'CORRECT_STEP',
        winner: 'X_WON',
        errorCode: null,
        errorMessage: null,
        traceId: null,
      })
    })
    expect(result.current.state.phase).toBe('finished')

    // A late stream error must not revive polling — terminal phases absorb everything but START.
    act(() => instance.onerror?.())
    vi.mocked(gameApiService.getSession).mockClear()
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000)
    })

    expect(result.current.state.phase).toBe('finished')
    expect(gameApiService.getSession).not.toHaveBeenCalled()
  })
})
