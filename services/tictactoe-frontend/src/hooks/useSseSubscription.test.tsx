import { StrictMode } from 'react'
import { renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useSseSubscription } from './useSseSubscription'
import { FakeEventSource } from '../test/fakeEventSource'

beforeEach(() => {
  FakeEventSource.instances = []
  vi.stubGlobal('EventSource', FakeEventSource)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const noopHandlers = { onEvent: vi.fn(), onError: vi.fn(), onOpen: vi.fn() }

describe('useSseSubscription', () => {
  it('opens exactly one connection per sessionId', () => {
    renderHook(() => useSseSubscription('session-1', noopHandlers))

    expect(FakeEventSource.instances).toHaveLength(1)
    expect(FakeEventSource.instances[0].url).toContain('/sessions/session-1/events')
  })

  it('closes the connection on unmount', () => {
    const { unmount } = renderHook(() => useSseSubscription('session-1', noopHandlers))
    const instance = FakeEventSource.instances[0]

    unmount()

    expect(instance.closed).toBe(true)
  })

  it('does not leak a second connection under StrictMode double-invocation', () => {
    renderHook(() => useSseSubscription('session-1', noopHandlers), { wrapper: StrictMode })

    const openConnections = FakeEventSource.instances.filter((instance) => !instance.closed)
    expect(openConnections).toHaveLength(1)
  })
})
