// vitest/config re-exports vite's defineConfig with the `test` option typed in — same runtime
// config either way, so this doesn't affect `vite`/`vite build`.
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// Minimal ambient type for the one Node global this file reads — avoids adding @types/node as a
// dependency just for this.
declare const process: { env: Record<string, string | undefined> }

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  test: {
    // No component rendering tests (see CLAUDE.md) — the suite is pure reducer/logic functions,
    // so the default Node environment is enough; no jsdom dependency needed.
    include: ['src/**/*.test.ts'],
  },
})
