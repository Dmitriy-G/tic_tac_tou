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

### Running in debug mode

Suspends the JVM until a debugger attaches, on a distinct port per service so both can run at once.

```bash
mvn spring-boot:run -pl services/tictactoe-engine -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
mvn spring-boot:run -pl services/tictactoe-session -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006"
```

On Windows PowerShell, `mvn.cmd` mangles the quoted `-D` value (`Illegal char <*>`) unless parsing is stopped first with `--%`:

```powershell
mvn spring-boot:run -pl services/tictactoe-engine --% -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
mvn spring-boot:run -pl services/tictactoe-session --% -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006"
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

## Error handling

Both services share one error shape and one code catalog (`com.tictactoe.common.error` in
`tictactoe-common`) for every fault — an unknown id, a malformed request, a dependency down. Rule
outcomes (`StepStatus` on a move) are a separate channel and never appear here — see
`docs/adr/0003-error-channels.md`.

Every response body: `{timestamp, status, code, message, path, traceId, fieldErrors?}`. `traceId`
is the `X-Correlation-Id` correlation id, echoed on the response header too.

| Code | HTTP status | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Request body/params failed `@Valid` constraints |
| `MALFORMED_REQUEST` | 400 | Body isn't valid JSON, or another `IllegalArgumentException` |
| `NOT_FOUND` | 404 | Generic not-found |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method for the path |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Wrong `Content-Type` |
| `CONFLICT` | 409 | Generic conflict |
| `TOO_MANY_REQUESTS` | 429 | Generic rate limit |
| `INTERNAL_ERROR` | 500 | Unexpected exception (catch-all) |
| `GAME_NOT_FOUND` | 404 | Engine: unknown `gameId` |
| `GAME_ALREADY_EXISTS` | 409 | Engine: reserved, not yet used |
| `INVALID_GAME_ID` | 400 | Engine: `gameId` path variable isn't a UUID |
| `SESSION_NOT_FOUND` | 404 | Session: unknown `sessionId` |
| `INVALID_SESSION_ID` | 400 | Session: reserved, not yet used |
| `SIMULATION_ALREADY_RUNNING` | 409 | Session: `/simulate` called while one is already running |
| `SESSION_ALREADY_COMPLETED` | 409 | Session: reserved, not yet used |
| `SIMULATION_LIMIT_REACHED` | 429 | Session: reserved, not yet used |
| `ENGINE_UNAVAILABLE` | 503 | Session→engine: timeout / connection refused / 5xx — retried |
| `ENGINE_STATE_LOST` | 502 | Session→engine: engine returned 404 for a game the session expects to exist |
| `ENGINE_CONTRACT_VIOLATION` | 500 | Session→engine: engine returned 400 — the session's bug, not the caller's |
| `ENGINE_BAD_RESPONSE` | 502 | Session→engine: unparseable or incomplete response |
| `SIMULATION_TIMEOUT` | 500 | Session: reserved, not yet used |
| `DATABASE_ERROR` | 503 | Either service: repository threw `DataAccessException` |

See `docs/adr/0004-downstream-status-mapping.md` for why the session service never passes an
engine HTTP status straight through to its own caller.

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
