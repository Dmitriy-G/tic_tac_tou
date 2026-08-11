# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This is a monorepo for a distributed Tic Tac Toe assignment where the game is played automatically by backend microservices while the UI observes and displays progress (see `task.MD` / `task.pdf` for the full spec).

```
client/                          # React + TypeScript UI — see client/CLAUDE.md
services/
  game-engine-service/           # Java + Spring Boot — board state, move validation, game outcome
  game-session-service/          # Java + Spring Boot — session management, automated move generation
```

Each subproject is independent (its own `package.json` / `pom.xml`, no shared parent build) and is run separately — see the root `README.md` for run commands. There is currently no build orchestration tying the three together.

## Service responsibilities

- **game-engine-service** (port 8081): owns board state per `gameId`. `POST /games/{gameId}/move` validates and applies a move, returning the updated game/status; `GET /games/{gameId}` returns current state. All classes are currently skeletons (`UnsupportedOperationException` bodies) — no board/move/win logic is implemented yet.
- **game-session-service** (port 8082): owns sessions. `POST /sessions` creates a session; `POST /sessions/{sessionId}/simulate` drives an automated game to completion by generating moves for both players and forwarding them to `game-engine-service` via `GameEngineClient`; `GET /sessions/{sessionId}` returns session state and move history. Also skeletons only.
- **client**: renders the board and status from data coming from `game-session-service` — see `client/CLAUDE.md` for its internal architecture (note: as of now its `App.tsx` still implements local click-to-play state rather than consuming the session service; that needs to be reworked to fit this spec).

## Conventions used across the Java services

- Package root: `com.tictactoe.<service-name-without-dashes>` (e.g. `com.tictactoe.gameengine`).
- Layout per service: `controller/`, `service/` (interface + `*Impl`), `model/`, `exception/`; `game-session-service` additionally has `client/` for the REST client to `game-engine-service`.
- Spring Boot 3.3.5, Java 21, `spring-boot-starter-web` only — no persistence dependency added yet (task allows a plain in-memory structure instead of H2).
