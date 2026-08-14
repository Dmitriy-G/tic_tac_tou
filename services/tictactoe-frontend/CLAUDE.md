# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm install` — install dependencies
- `npm run dev` — start the Vite dev server with HMR
- `npm run build` — type-check via `tsc -b` then production build with Vite (`dist/`)
- `npm run type-check` — `tsc -b` only, no build (what CI and the root `CLAUDE.md` expect to exist)
- `npm run preview` — serve the production build locally
- `npm run lint` — run ESLint over the whole project
- `npm run test` — run the Vitest suite once (CI mode)
- `npm run test:watch` — run Vitest in watch mode

Test runner is Vitest (`vite.config.ts`'s `test` block, Node environment — no `jsdom`, since the
suite is pure functions and deliberately has no component-rendering tests: `*.test.ts` next to the
module it covers in `src/state/` and `src/utils/`). This is the one exception to "no new runtime
dependencies" in the assignment's constraints — `vitest` is a devDependency only, added because
the assignment's own testing requirements name frontend tests.

## Architecture

Tic Tac Toe UI built with React 19 + TypeScript, scaffolded on Vite. State management is a single
`useReducer` over a discriminated union plus three single-purpose hooks — no external state
library (see "Deviations from the root CLAUDE.md" below).

- `src/utils/types.ts` and `src/utils/logic.ts` hold game rules as pure, UI-independent functions.
  `calculateWinner` is the one rule computation left in the frontend, kept only to derive the
  winning triple for the board highlight — the engine is still the sole authority on outcomes;
  the API just doesn't return `winningLine` yet. `createEmptyBoard`, `normalizeBoard`, and
  `findChangedIndex` (recovers which cell an SSE event changed, since `SessionEvent` carries a
  board snapshot but no `position`) round out the module.
- `src/state/simulationReducer.ts` — the state machine for one simulation run:
  `SimulationState` is a discriminated union on `phase` (`idle | starting | running |
  reconnecting | finished | failed`), each variant carrying exactly the data valid for that
  phase (`finished` carries `outcome`, `failed` carries `message`, nothing else does). This
  replaces what used to be four independently-settable flags (`status`, `isSimulating`,
  `isReconnecting`, `error`) plus a `boardRef` shadowing `squares` — nothing can express
  `isSimulating === true` alongside a terminal status because the type doesn't have a slot for
  that combination. Terminal phases (`finished`/`failed`) are absorbing: the reducer ignores every
  action except `START` once in one of them, which is what stops a late poll response from
  reviving a session the UI already reported as done. `isSimulating` is derived
  (`phase === 'starting' || 'running' || 'reconnecting'`), never stored.
- `src/hooks/useSseSubscription.ts` — owns the `EventSource` lifecycle only: opens on a session
  id, forwards parsed events to the given handlers, closes on unmount or id change.
- `src/hooks/useSessionPolling.ts` — `setInterval` over `GET /sessions/{id}` while `enabled`.
  Knows nothing about SSE; `useSimulation` enables it only while `phase === 'reconnecting'`.
- `src/hooks/useSimulation.ts` — composes the two hooks above with the reducer, owns the
  reconnect-timeout that gives a dropped stream 8s to recover before the run is marked failed,
  and exposes `{ state, isSimulating, start }`. This is the only hook `App` calls.
- `src/services/gameApiService.ts` talks to `tictactoe-session`: `createSession`/`startSimulation`
  over REST, `getSession` for polling/rehydration, `subscribeToSession` over SSE
  (`GET /sessions/{id}/events`).
- `src/App.tsx` calls `useSimulation` and renders structure only — `StatusBanner`, the
  reconnecting/error lines, `Board`, the start button, `MoveLog`. It owns no state itself.
- `src/components/Board.tsx` renders a `role="grid"` of 9 `Square`s grouped into `role="row"`
  wrappers (via inline `display: contents`, so the CSS grid layout — unchanged — still treats the
  9 squares as direct grid items) and highlights cells listed in the winning line. Index keys are
  correct here: a fixed-length positional array that never reorders.
- `src/components/Square.tsx` is a single presentational cell, wrapped in `memo` since a board
  update only ever changes one of nine.
- `src/components/StatusBanner.tsx` — the status line as a `phase → text` lookup, with a small
  branch for the `finished` phase's outcome (win/draw/cancelled). `aria-live="polite"`, since nothing
  else announces the automatic updates to a screen reader.
- `src/components/MoveLog.tsx` — formats `MoveHistoryEntry[]` (the same shape
  `SessionResponse.moves` returns) at render time via `formatMoveLogEntry`, keyed on
  `moveNumber`. Because the reducer's `SNAPSHOT` action replaces `moves` from the polled
  session's own history, a move log survives a dropped-and-recovered stream — the old
  string-log design lost anything that arrived while polling was covering for SSE.
- `src/components/ErrorBoundary.tsx` — wraps `<App />` in `main.tsx`; a render throw shows a
  message instead of a white screen.

TypeScript project uses project references: `tsconfig.json` points to `tsconfig.app.json` (app
code, `src/`) and `tsconfig.node.json` (`vite.config.ts`). `npm run build` runs `tsc -b` first, so
type errors fail the build before Vite bundles.

## Deviations from the root CLAUDE.md

Both noted here rather than silently: the root doc describes the target stack for the whole
system, sized for a larger app than this single view.

- **ESLint, not Biome.** The root `CLAUDE.md` calls for Biome; this project's ESLint config
  (`eslint.config.js`, flat-config, `typescript-eslint` + `eslint-plugin-react-hooks` +
  `eslint-plugin-react-refresh`) is already tuned for React 19 and working. Migrating buys a
  reviewer nothing here, so the root doc has been amended to point at this file instead of
  mandating Biome for the frontend.
- **No `@tanstack/react-query`.** The root doc's target stack includes it for REST state; this
  app makes three calls total, already handled by `gameApiService.ts` plus
  `useSimulation`/`useSessionPolling`. Adding a data-fetching library would be dependency weight
  with no reviewer-visible benefit. No state library, no Tailwind, either — same reasoning.

There are no other placeholder directories: `src/views/` and `src/config/` (empty, reserved by
the original target layout) were removed — this is a single view and was not going to grow into a
multi-page app during the assignment, and empty directories are noise a reviewer has to rule out.
`src/hooks/` and `src/state/` are populated, not placeholders.
