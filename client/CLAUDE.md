# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm install` — install dependencies
- `npm run dev` — start the Vite dev server with HMR
- `npm run build` — type-check via `tsc -b` then production build with Vite (`dist/`)
- `npm run preview` — serve the production build locally
- `npm run lint` — run ESLint over the whole project

There is no test runner configured in this project yet.

## Architecture

Tic Tac Toe UI built with React 19 + TypeScript, scaffolded on Vite.

- `src/game/types.ts` and `src/game/logic.ts` hold all game rules as pure, UI-independent functions (`calculateWinner`, `isBoardFull`, `createEmptyBoard`, `WINNING_LINES`). Any change to win/draw logic belongs here, not in components.
- `src/App.tsx` owns all game state (`squares`, `xIsNext`) via `useState` and derives `winResult`/`isDraw`/`gameOver` from `src/game/logic.ts` on each render — there is no reducer or external state library. `App` is the only component that mutates state; `Board` and `Square` are presentational and receive callbacks/props down.
- `src/components/Board.tsx` renders 9 `Square`s from the flat `squares` array and highlights cells listed in the winning line.
- `src/components/Square.tsx` is a single controlled button; it disables itself once filled or once `gameOver` is passed down from `App`.

TypeScript project uses project references: `tsconfig.json` points to `tsconfig.app.json` (app code, `src/`) and `tsconfig.node.json` (`vite.config.ts`). `npm run build` runs `tsc -b` first, so type errors fail the build before Vite bundles.

ESLint (`eslint.config.js`) is flat-config based, combining `typescript-eslint` recommended rules with `eslint-plugin-react-hooks` and `eslint-plugin-react-refresh` (Vite-specific fast-refresh rule).