# Frontend (`tictactoe-frontend`)

React + TypeScript UI that starts a simulation and renders the board, status, and move history as `tictactoe-session` and `tictactoe-engine` play the game out automatically.

## Prerequisites

- [Node.js](https://nodejs.org/) 20+ (LTS recommended)
- npm (comes with Node.js)
- `tictactoe-session` running and reachable (see `VITE_SESSION_SERVICE_URL`, defaults to `http://localhost:8082`)

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
| `npm run preview` | Preview the production build locally          |
| `npm run lint`    | Run ESLint                                    |

## Project structure

```
src/
  main.tsx             # App entry point
  App.tsx              # Game state: turns, win/draw detection, SSE subscription
  components/
    Board.tsx           # 3x3 grid
    Square.tsx           # Single cell
    MoveLog.tsx          # Move history list
  services/
    gameApiService.ts    # REST + SSE calls to tictactoe-session
  utils/
    types.ts             # Player, SquareValue, BoardState, GameSession, SessionEvent types
    logic.ts              # Win detection and board helpers
    moveLog.ts             # Move log formatting
  styles/
    App.css, index.css
  views/                # placeholder — no route-level views yet
  hooks/                # placeholder — no custom hooks yet
  config/               # placeholder — no runtime config module yet
```

## Status

`App.tsx` still owns the current game/session state directly with `useState`/`useRef`. The root `CLAUDE.md` calls for `@tanstack/react-query` for REST calls, Biome instead of ESLint/Prettier, an SSE-error fallback to polling, and refresh-mid-simulation rehydration — none of that is implemented yet; this module has only been reorganized to match the target directory layout.
