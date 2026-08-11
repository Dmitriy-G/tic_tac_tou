# Tic Tac Toe

A simple Tic Tac Toe game UI built with React, TypeScript, and Vite.

## Prerequisites

- [Node.js](https://nodejs.org/) 20+ (LTS recommended)
- npm (comes with Node.js)

## Getting started

Run these commands from this directory (`client`):

```bash
cd client
npm install
```

Start the dev server (with hot reload):

```bash
cd client
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
  main.tsx           # App entry point
  App.tsx            # Game state: turns, win/draw detection, reset
  components/
    Board.tsx         # 3x3 grid
    Square.tsx          # Single cell
  game/
    types.ts            # Player, SquareValue, BoardState types
    logic.ts             # Win detection and board helpers
```
