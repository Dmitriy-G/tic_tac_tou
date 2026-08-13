import { defineConfig } from 'vite'
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
})
