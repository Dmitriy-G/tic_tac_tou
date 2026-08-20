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
    // jsdom backs the hook/component tests (Testing Library needs a DOM); the pure reducer/logic
    // tests run fine under it too, so one environment covers the whole suite.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
