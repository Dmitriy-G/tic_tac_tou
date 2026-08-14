# Frontend (`tictactoe-frontend`)

React + TypeScript UI that starts a simulation and renders the board, status, and move history as `tictactoe-session` and `tictactoe-engine` play the game out automatically.

## Prerequisites

- [Node.js](https://nodejs.org/) 20+ (LTS recommended)
- npm (comes with Node.js)
- `tictactoe-session` running and reachable — see "Talking to the session service" below.

## Talking to the session service

The UI never hardcodes `tictactoe-session`'s host or port, and there is no CORS layer — both
`npm run dev` and the built container reach it through the **same relative `/api/` prefix**, per
`docs/adr/0001-edge-routing-no-gateway.md`. `gameApiService.ts` calls `/api/sessions`, `/api/sessions/{id}/events`, etc.; something else always turns that into a real request:

- **In the built container**, nginx (`nginx.conf`) proxies `/api/` to `tictactoe-session:8082`,
  stripping the prefix.
- **In `npm run dev`**, Vite's own dev server proxy (`vite.config.ts`, `server.proxy['/api']`)
  does the same thing, forwarding to `http://localhost:8082` by default (override with
  `VITE_PROXY_TARGET` if the session service runs somewhere else).

**These two rewrites must stay in step.** Both strip exactly the `/api` prefix and forward
everything else unchanged; if one is edited without the other, `npm run dev` and the built
container disagree on where `/api/sessions` ends up; the container is what runs by the test
suite/deployment, so a dev-only breakage here is easy to miss.

## Getting started

Run these commands from this directory (`services/tictactoe-frontend`):

```bash
cd services/tictactoe-frontend
npm install
npm run dev
```

Then open the URL printed in the terminal (usually [http://localhost:5173](http://localhost:5173)).

## Other scripts

| Command           | Description                                   |
| ----------------- | --------------------------------------------- |
| `npm run dev`     | Start the local dev server                    |
| `npm run build`   | Type-check and build for production (`dist/`) |
| `npm run type-check` | `tsc -b` only, no build                    |
| `npm run preview` | Preview the production build locally          |
| `npm run lint`    | Run ESLint                                    |
| `npm run test`    | Run the Vitest suite once                     |
| `npm run test:watch` | Run Vitest in watch mode                   |

## Project structure

```
src/
  main.tsx             # Entry point; wraps App in ErrorBoundary inside StrictMode
  App.tsx              # Structure only — calls useSimulation, renders the components below
  state/
    simulationReducer.ts # The one simulation run's state machine (see CLAUDE.md)
  hooks/
    useSimulation.ts     # Composes the two hooks below with the reducer; the only hook App calls
    useSseSubscription.ts # Owns the EventSource lifecycle
    useSessionPolling.ts  # Polling fallback while the stream is down
  components/
    Board.tsx           # 3x3 grid (role="grid" / role="row" / role="gridcell")
    Square.tsx           # Single presentational cell, memoized
    MoveLog.tsx          # Move history list, formatted from structured entries at render time
    StatusBanner.tsx     # phase -> status text
    ErrorBoundary.tsx    # Catches render throws
  services/
    gameApiService.ts    # REST + SSE calls to tictactoe-session
  utils/
    types.ts             # Player, SquareValue, BoardState, SessionResponse, SessionEvent, ApiErrorBody types
    logic.ts              # Win detection and board helpers
    moveLog.ts             # Move log formatting
  styles/
    App.css, index.css
```

See this module's `CLAUDE.md` for how the pieces compose and the two recorded deviations from the
root `CLAUDE.md` (ESLint instead of Biome, no `@tanstack/react-query`).

## Status

SSE error handling: `onerror` does not close the stream immediately (the browser reconnects on
its own), a reconnecting indicator shows while polling `GET /api/sessions/{id}` as a fallback, and
the stream only gives up after a bounded timeout (8s), at which point the run is marked failed.
The `failure` SSE event and `GET /api/sessions/{id}` error fields (`errorCode`/`errorMessage`)
both surface in the error banner. The move log is rebuilt from `session.moves` on every poll
snapshot, so it survives a stream drop instead of losing whatever arrived while SSE was down.
Refresh-mid-simulation rehydration (restoring `sessionId` itself after a reload) is not
implemented — there is nowhere client-side to read it back from, since `CLAUDE.md` forbids
`localStorage`/`sessionStorage` and the session id is not in the URL.
