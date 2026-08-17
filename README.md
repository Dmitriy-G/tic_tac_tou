# Distributed Tic Tac Toe

![CI](https://github.com/Dmitriy-G/tic_tac_tou/actions/workflows/ci.yml/badge.svg)

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

See `CLAUDE.md` for the full API contract, domain rules, and conventions;

## Running everything

```bash
docker compose up --build
```

Everything is reachable from one origin: **http://localhost:5173**. The frontend's nginx serves
the built UI at `/` and reverse-proxies `/api/*` to the session service — there is no separate API
host to configure, and no CORS layer exists anywhere (same origin doesn't need one). See
`docs/adr/0001-edge-routing-no-gateway.md`.

## Endpoints

| Method | Path (via `:5173`, browser-facing) | Path (direct, for `curl`/Swagger) | Description |
| --- | --- | --- | --- |
| `POST` | `/api/sessions` | `tictactoe-session:8082/sessions` | Create a session |
| `POST` | `/api/sessions/{id}/simulate` | `tictactoe-session:8082/sessions/{id}/simulate` | Start the automated simulation (async, owner-only — see Security below) |
| `GET` | `/api/sessions/{id}` | `tictactoe-session:8082/sessions/{id}` | Full session view — status, board, moves, winner, error |
| `GET` | `/api/sessions/{id}/events` | `tictactoe-session:8082/sessions/{id}/events` | SSE stream of board updates |
| — | — | `tictactoe-engine:8081/games...` | Engine API — never called by the browser, only by the session service |

`tictactoe-session:8082` stays published to the host for inspection and Swagger UI
(`http://localhost:8082/swagger-ui.html`). The engine and both Postgres databases are **not**
published — see Security below for how to reach them anyway.

## Security

Copy `.env.example` to `.env` and set `INTERNAL_TOKEN` before running `docker compose up` — both
services fail to start without it. See `docs/adr/0002-security-model.md` for the full model
(network exposure, the engine's internal-token boundary, and session ownership).

`POST /sessions` returns an owner token and also sets it as an `HttpOnly` cookie
(`tictactoe_session_owner`, `Path=/api`). The browser sends it automatically on subsequent
requests; a `curl` user must capture and replay it explicitly with `-c`/`-b`, or the first
`/simulate` call gets a `403 NOT_SESSION_OWNER`:

```bash
curl -s -c jar.txt -XPOST http://localhost:5173/api/sessions | jq .
SID=<sessionId from the response above>
curl -s -b jar.txt -XPOST http://localhost:5173/api/sessions/$SID/simulate
curl -s http://localhost:5173/api/sessions/$SID   # GET is open, no cookie needed
```

The engine and both databases are reachable only from inside the Compose network by default. To
inspect them manually (e.g. the engine's Swagger UI), bring up the debug profile, which
republishes the engine on `8081` and both databases on `5433`/`5434` via `socat` sidecars:

```bash
docker compose --profile debug up -d
curl http://localhost:8081/actuator/health
```

The same debug profile also republishes both databases, for a SQL client (DataGrip, DBeaver, or
`psql` from the host):

| Database | Host | Port | User | Password |
| --- | --- | --- | --- | --- |
| `engine_db` | `localhost` | `5433` | `engine` | `engine` |
| `session_db` | `localhost` | `5434` | `session` | `session` |

```bash
docker compose --profile debug up -d
psql -h localhost -p 5433 -U engine engine_db

docker compose --profile debug down    # closes the debug ports again; services keep running
```

If you only need a quick query, no port is required at all — `psql` can run inside the container:

```bash
docker compose exec engine-db psql -U engine engine_db
docker compose exec session-db psql -U session session_db
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
```

## CI

`.github/workflows/ci.yml` runs on every push (any branch) and every pull request, with a
concurrency group that cancels superseded runs on the same ref. Two jobs run in parallel so a
frontend lint failure never hides a backend test failure:

- **Backend** — SpotBugs static analysis (`mvn spotbugs:check`, threshold `High`) followed by
  `mvn verify` (compiles, runs the full JUnit suite, and would run Failsafe `*IT` integration
  tests). Surefire reports are uploaded as an artifact even when the build fails.
- **Frontend** — `npm ci`, ESLint (`--max-warnings 0`, so warnings fail the build), `tsc` type
  checking, the Vitest suite, and the production build.

A third job, **Docker images build**, runs `docker compose build` after both of the above pass, to
catch a Dockerfile path broken by a module rename; it does not start the containers or hit the API
— that is the integration test's job, not CI's.

No path filtering: with three Maven modules and one frontend, the whole suite runs in a couple of
minutes, so every commit runs everything. No secrets are required — `INTERNAL_TOKEN` is only used
at runtime and the test profile (`application-test.yml`) supplies its own value.

## Error handling

Both services share one error shape and one code catalog (`com.tictactoe.common.error` in
`tictactoe-common`) for every fault — an unknown id, a malformed request, a dependency down. Rule
outcomes (`StepStatus` on a move) are a separate channel and never appear here.

Every response body: `{timestamp, status, code, message, path, traceId, fieldErrors?}`. `traceId`
is the `X-Correlation-Id` correlation id, echoed on the response header too.

| Code | HTTP status | Meaning |
| --- | --- | --- |
| `VALIDATION_ERROR` | 400 | Request body/params failed `@Valid` constraints |
| `MALFORMED_REQUEST` | 400 | Body isn't valid JSON, or another `IllegalArgumentException` |
| `UNAUTHORIZED` | 401 | Engine: missing/invalid `X-Internal-Token` (session→engine boundary only) |
| `NOT_SESSION_OWNER` | 403 | Session: `/simulate` called without the creating session's owner cookie |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method for the path |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Wrong `Content-Type` |
| `INTERNAL_ERROR` | 500 | Unexpected exception (catch-all) |
| `GAME_NOT_FOUND` | 404 | Engine: unknown `gameId` |
| `INVALID_GAME_ID` | 400 | Engine: `gameId` path variable isn't a UUID |
| `SESSION_NOT_FOUND` | 404 | Session: unknown `sessionId` |
| `INVALID_SESSION_ID` | 400 | Session: `sessionId` path variable isn't a UUID |
| `SIMULATION_ALREADY_RUNNING` | 409 | Session: `/simulate` called while one is already running |
| `SESSION_ALREADY_COMPLETED` | 409 | Session: `/simulate` called on a session that already reached a terminal state |
| `ENGINE_UNAVAILABLE` | 503 | Session→engine: timeout / connection refused / 5xx — retried |
| `ENGINE_STATE_LOST` | 502 | Session→engine: engine returned 404 for a game the session expects to exist |
| `ENGINE_CONTRACT_VIOLATION` | 500 | Session→engine: engine returned 400 — the session's bug, not the caller's |
| `ENGINE_BAD_RESPONSE` | 502 | Session→engine: unparseable or incomplete response |
| `DATABASE_ERROR` | 503 | Either service: repository threw `DataAccessException` |

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
