# Distributed Tic Tac Toe

A distributed Tic Tac Toe application in which the game is played automatically by microservices, with a UI that watches the game unfold.

## Components

| Component | Path | Stack | Port |
| --- | --- | --- | --- |
| Game Engine Service | `services/tictactoe-engine` | Java, Spring Boot | 8081 |
| Game Session Service | `services/tictactoe-session` | Java, Spring Boot | 8082 |
| Frontend | `services/tictactoe-frontend` | React, TypeScript | 5173 (dev) |

- **Game Engine Service** — owns the board, validates moves, and determines the game outcome (in progress, win, or draw). Sole authority on game rules.
- **Game Session Service** — creates and manages game sessions, automates moves for both players, and coordinates with the Game Engine Service.
- **Frontend** — starts a simulation and renders the board, status, and move history as the microservices play against each other, via SSE with a polling fallback.

See `CLAUDE.md` for the full API contract, domain rules, and conventions; `task.MD` / `task.pdf` for the original assignment spec.

## Running everything

```bash
docker compose up --build
```

## Running each service individually

The two Java services share a root `pom.xml` aggregator.

### Game Engine Service

```bash
mvn spring-boot:run -pl services/tictactoe-engine
```

### Game Session Service

```bash
mvn spring-boot:run -pl services/tictactoe-session
```

### Frontend

```bash
cd services/tictactoe-frontend
npm install
npm run dev
```

## Building and testing

```bash
mvn clean install                          # build + test both Java services
mvn test -pl services/tictactoe-engine      # one module

cd services/tictactoe-frontend
npm run type-check
npm run lint:biome
```

## Structure

```
.
├── pom.xml                              # aggregator, all versions in <properties>
├── docker-compose.yml
├── README.md  CLAUDE.md
├── services/
│   ├── tictactoe-engine/                # Java + Spring Boot — board state, move validation, outcome
│   ├── tictactoe-session/               # Java + Spring Boot — session lifecycle, move generation
│   └── tictactoe-frontend/              # React + TypeScript — board rendering, SSE, move log
└── docs/
    ├── adr/                             # decision records
    ├── development/{setup,testing}/
    └── diagrams/                        # Mermaid HLD + sequence diagrams
```

## Status

Structural scaffolding matches the target layout in `CLAUDE.md`, but most business logic is not
implemented yet: the engine's board/move/win logic, the session service's move strategies and
resilience (timeouts/retry/circuit breaker), and the frontend's React Query + Biome migration are
all still open. See each module's README for exactly what's stubbed vs. working.
