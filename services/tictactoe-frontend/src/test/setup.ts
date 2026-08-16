import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Testing Library's own auto-cleanup only registers itself when it finds a global `afterEach`
// (Jest's default). Vitest doesn't put one on `globalThis` unless `test.globals` is enabled, which
// this project deliberately doesn't do — so register cleanup explicitly instead.
afterEach(() => {
  cleanup()
})
