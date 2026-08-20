/** Minimal EventSource stand-in for tests: records listeners per event name and lets the test
 * emit events/errors/opens on demand, without a real network connection. */
export class FakeEventSource {
  static instances: FakeEventSource[] = []

  readonly url: string
  closed = false
  onopen: (() => void) | null = null
  onerror: (() => void) | null = null
  private listeners: Record<string, Array<(event: MessageEvent<string>) => void>> = {}

  constructor(url: string) {
    this.url = url
    FakeEventSource.instances.push(this)
  }

  addEventListener(type: string, listener: (event: MessageEvent<string>) => void) {
    ;(this.listeners[type] ??= []).push(listener)
  }

  close() {
    this.closed = true
  }

  emit(type: string, data: unknown) {
    const event = { data: JSON.stringify(data) } as MessageEvent<string>
    this.listeners[type]?.forEach((listener) => listener(event))
  }
}
