# Game Session Service (`tictactoe-session`)

Manages game session lifecycle and automates gameplay by generating moves for both players, coordinating with the Game Engine Service. The engine remains the sole authority on game rules — this service never keeps its own copy of the board.

## Prerequisites

- Java 21+
- Maven 3.9+
- The Game Engine Service running and reachable (see `game-engine.base-url` in `src/main/resources/application.yml`, defaults to `http://localhost:8081`, overridable via `ENGINE_BASE_URL`)
- A PostgreSQL 16 database reachable at startup — `docker compose up -d session-db` from the repository root, or point `SESSION_DB_URL` at your own instance. Schema is created by Flyway on boot (`src/main/resources/db/migration`); there is no `ddl-auto` fallback.

## Configuration

| Env var | Default | Purpose |
| --- | --- | --- |
| `SESSION_DB_URL` | `jdbc:postgresql://localhost:5434/session_db` | JDBC URL for `session_db` |
| `SESSION_DB_USERNAME` | `session` | Database username |
| `SESSION_DB_PASSWORD` | `session` | Database password |
| `ENGINE_CONNECT_TIMEOUT_MS` | `2000` | Connect timeout for calls to the engine |
| `ENGINE_READ_TIMEOUT_MS` | `3000` | Read timeout for calls to the engine |
| `SIMULATION_MOVE_DELAY_MS` | `500` | Delay between simulated moves, so the UI shows progression (tests set this to `0`) |
| `SIMULATION_STRATEGY_X` | `SIMPLE` | Move strategy for `X`: `SIMPLE` (random empty cell) or `ADVANCED` (not implemented yet) |
| `SIMULATION_STRATEGY_O` | `SIMPLE` | Move strategy for `O`: `SIMPLE` (random empty cell) or `ADVANCED` (not implemented yet) |
| `ENGINE_RETRY_MAX_ATTEMPTS` | `3` | Max attempts (including the first) for a call to the engine, retried only when it fails with `ENGINE_UNAVAILABLE` |
| `ENGINE_RETRY_INITIAL_BACKOFF_MS` | `200` | Base backoff between retries (Resilience4j exponential backoff with jitter) |
| `SSE_HEARTBEAT_INTERVAL_MS` | `15000` | Interval between `:ping` SSE comment frames per subscriber — keeps idle timers (this service's, any reverse proxy's, the browser's) from expiring during a quiet stretch. Below the smallest timeout in the path; see `docs/adr/0005-edge-routing-no-gateway.md` |

## Running

From the repository root (this module is part of the aggregator `pom.xml`):

```bash
docker compose up -d session-db
mvn spring-boot:run -pl services/tictactoe-session
```

Or build a jar and run it directly:

```bash
mvn clean package -pl services/tictactoe-session -am
java -jar services/tictactoe-session/target/tictactoe-session-0.0.1-SNAPSHOT.jar
```

The service starts on port `8082` (see `src/main/resources/application.yml`).

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/sessions` | Create a new game session |
| `POST` | `/sessions/{sessionId}/simulate` | Trigger the automated simulation of a game until it concludes (async) |
| `GET` | `/sessions/{sessionId}` | Retrieve session details, including game state and move history |
| `GET` | `/sessions/{sessionId}/events` | Server-Sent Events stream of board updates and status |
| `POST` | `/sessions/{sessionId}/cancel` | Cancel a running simulation |

`sessionId` **is** the `gameId` returned by the engine (single UUID, one session = one game). See the root `CLAUDE.md` for the full request/response contract and status code table.

## API documentation

Swagger UI: `http://localhost:8082/swagger-ui.html`
OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Storage

`session_db` (own PostgreSQL database, never shared with the engine) holds two tables, both managed by Flyway (`src/main/resources/db/migration/V1__create_sessions_and_simulations_tables.sql`):

- `sessions (id UUID)` — a durable identity/anchor row created once per session. It intentionally does **not** store `gameId` (identical to `id` — `sessionId` **is** `gameId`), the board (the engine's authoritative copy is never duplicated here), or move history (would duplicate history the engine already owns — see the root `CLAUDE.md` anti-patterns).
- `simulations (id UUID, session_id UUID FK -> sessions, errors_count INT, started_at TIMESTAMP, finished_at TIMESTAMP, status VARCHAR, running_session_id UUID, error_code VARCHAR, error_message VARCHAR)` — one row per `/simulate` run, the durable audit trail of run outcomes. `SimulationStarter`/`SimulationStateWriter` write and update it as a run progresses; `error_code`/`error_message` (V3 migration) are set when a run ends in `FAILED`. `running_session_id` mirrors `session_id` only while `status = 'IN_PROGRESS'` and is otherwise `null`; a unique index on it (V2 migration) is what actually stops two concurrent `/simulate` calls for the same session from both starting — H2 (used in tests) has no partial-index support, so this mirror-column trick gives the same guarantee a Postgres partial index would, portably.

A session's *live* board/move history while a simulation is running is process-local state, held in `SessionStateStore` (`Map<String, LiveState>`, mutated only through `compute()`) — never written to `session_db`, per the root `CLAUDE.md`. `GET /sessions/{id}` is built entirely from this store, which is what makes the SSE stream an optimisation rather than a dependency: `SimulationEventPublisher` updates the store and publishes the SSE event from the same call, so both are always in sync. On a fresh JVM (e.g. after a restart) the store is empty and a session reads back as `CREATED` with an empty board.

## Status

`simulate()` drives real gameplay: it creates the game at the engine (`gameId` == `sessionId`), then alternates `X`/`O` moves — the cell for each move chosen by the `MoveStrategy` configured for that symbol (`simulation.strategy.x` / `simulation.strategy.o`) — publishing an SSE event and updating `SessionStateStore` after every move, until the engine reports a terminal status or the 9-move hard cap is hit. Only the `SIMPLE` strategy (`RandomMoveStrategy`, a uniformly random empty cell) is implemented; `ADVANCED` (`RuleBasedMoveStrategy`) is still a skeleton (`UnsupportedOperationException`). `cancel()` is not implemented yet.

## Error handling

`SessionExceptionHandler` (`@RestControllerAdvice`) extends `BaseGlobalExceptionHandler` from `tictactoe-common` — see the root README's error-code table and `docs/adr/0003-error-channels.md` / `docs/adr/0004-downstream-status-mapping.md`.

- `SessionNotFoundException` → `404 SESSION_NOT_FOUND` on `GET`/`simulate`/`events` for an unknown session.
- `SimulationStarter.start` → `409 SIMULATION_ALREADY_RUNNING` if a simulation for the session is already `IN_PROGRESS`; the check is a fast-path repository query backed by the `running_session_id` unique index above as the actual race guard, so two concurrent `/simulate` calls always produce exactly one `202` and one `409`.
- `GameEngineClientImpl` classifies every engine-call failure into a typed exception — never a downstream status pass-through — and retries only `EngineUnavailableException` (timeout/connection-refused/5xx), up to `ENGINE_RETRY_MAX_ATTEMPTS` with exponential backoff and jitter. A move is idempotent per `(gameId, symbol, position)`, so retrying after a timeout is safe.
- `SimulationRunner.run` guarantees a session is never left `IN_PROGRESS`: on any `RuntimeException` it persists a `FAILED` row with `error_code`/`error_message`, publishes a `failure` SSE event, and always completes the SSE emitter in a `finally` — persist, notify, release, in that order.
- `CorrelationIdFilter` reads/generates `X-Correlation-Id`, puts it in MDC as `traceId`, and echoes it on the response; `GameEngineClientImpl` forwards the same id to the engine, and `SimulationStarter` captures it (MDC doesn't cross `Thread.ofVirtual().start()`) so the background simulation's logs and SSE failure events carry it too.