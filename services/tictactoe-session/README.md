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
| `SIMULATION_STRATEGY_X` | `SIMPLE` | Move strategy for `X`: `SIMPLE` (random empty cell) or `ADVANCED` (win &gt; block &gt; center &gt; corner &gt; side) |
| `SIMULATION_STRATEGY_O` | `SIMPLE` | Move strategy for `O`: `SIMPLE` (random empty cell) or `ADVANCED` (win &gt; block &gt; center &gt; corner &gt; side) |
| `ENGINE_RETRY_MAX_ATTEMPTS` | `3` | Max attempts (including the first) for a call to the engine, retried only when it fails with `ENGINE_UNAVAILABLE` |
| `ENGINE_RETRY_INITIAL_BACKOFF_MS` | `200` | Base backoff between retries (Resilience4j exponential backoff with jitter) |
| `SSE_HEARTBEAT_INTERVAL_MS` | `15000` | Interval between `:ping` SSE comment frames per subscriber — keeps idle timers (this service's, any reverse proxy's, the browser's) from expiring during a quiet stretch. Below the smallest timeout in the path; see `docs/adr/0001-edge-routing-no-gateway.md` |
| `INTERNAL_TOKEN` | *(none — required)* | Shared secret sent as `X-Internal-Token` on every call to the engine. No default; startup fails if unset. See `docs/adr/0002-security-model.md` |
| `SECURE_COOKIES` | `false` | Sets the `Secure` attribute on the session-owner cookie; enable when served over HTTPS |
| `OWNER_COOKIE_MAX_AGE` | `PT2H` | `Duration` string for how long the owner cookie stays valid |

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
| `POST` | `/sessions` | Create a new game session. Response body includes `ownerToken` (once, only here) and the response also sets it as an `HttpOnly` cookie |
| `POST` | `/sessions/{sessionId}/simulate` | Trigger the automated simulation of a game until it concludes (async). Owner-only — requires the `tictactoe_session_owner` cookie from creation, else `403 NOT_SESSION_OWNER` |
| `GET` | `/sessions/{sessionId}` | Retrieve session details, including game state and move history. Open to anyone with the id |
| `GET` | `/sessions/{sessionId}/events` | Server-Sent Events stream of board updates and status. Open to anyone with the id |
| `POST` | `/sessions/{sessionId}/cancel` | Cancel a running simulation |

`sessionId` **is** the `gameId` returned by the engine (single UUID, one session = one game). See the root `CLAUDE.md` for the full request/response contract and status code table, and `docs/adr/0002-security-model.md` for the ownership model — one owner (holds the token, can simulate), many observers (hold only the id, can watch).

## API documentation

Swagger UI: `http://localhost:8082/swagger-ui.html`
OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Storage

`session_db` (own PostgreSQL database, never shared with the engine) holds two tables, managed by
Flyway (`src/main/resources/db/migration`, most recently `V5__collapse_simulations_into_sessions.sql`).
One session is one game — `sessionId` **is** `gameId` — so `sessions` is the single source of
truth for that game's state, not just an identity row; see `docs/adr/0004-session-is-the-game.md`.

- `sessions (id UUID, owner_token_hash VARCHAR(64), status VARCHAR(20), board VARCHAR(9), winner VARCHAR(1), errors_count INT, error_code VARCHAR(64), error_message VARCHAR(1000), started_at TIMESTAMP, finished_at TIMESTAMP)` — created once per session (`status = CREATED`, empty board), then updated in place by `SessionStateWriter` as the simulation progresses and by `SessionJpaRepository.claimForSimulation` when it starts. It intentionally does **not** store the board as a separate copy of anything the engine doesn't already own — the engine remains authoritative, this column is just where its response gets written after each move. `owner_token_hash` (V4 migration) is a SHA-256 hash of the session's owner token — the raw token is never persisted, only returned once at creation; see `docs/adr/0002-security-model.md`.
- `session_moves (session_id UUID FK -> sessions, move_number SMALLINT, symbol VARCHAR(1), position SMALLINT, step_status VARCHAR(32), created_at TIMESTAMP, PRIMARY KEY (session_id, move_number))` — the durable, replayable move history. Only accepted moves (`CORRECT_STEP`) get a row: a rejected attempt doesn't advance `move_number`, so the composite primary key would reject a second row at the same number — by design, not as an edge case to work around.

There is no more process-local `SessionStateStore`. `GET /sessions/{id}` is built entirely from
`sessions` + `session_moves`, which is what makes it survive a restart: a fresh JVM reads back the
same terminal state a session reached before the process died, instead of reporting `CREATED` with
an empty board. The SSE stream (`SimulationEventPublisher`/`SseEmitterRegistry`) remains a live
optimisation layered on top, not a dependency — `GET` and SSE both ultimately reflect the same
database rows, just on different latency budgets.

Known trade-off: persistence happens on every move (one `INSERT` into `session_moves` plus one
`UPDATE` on `sessions`), which is durable and fine for a nine-move game but would not scale to
batch/mass simulation — see the ADR's Consequences section.

## Status

`simulate()` drives real gameplay: `SimulationStarter.start` claims the session via
`SessionJpaRepository.claimForSimulation` (a compare-and-swap `UPDATE ... WHERE status =
'CREATED'`), then `SimulationRunner` creates the game at the engine (`gameId` == `sessionId`, the
same UUID throughout) and alternates `X`/`O` moves — the cell for each move chosen by the
`MoveStrategy` configured for that symbol (`simulation.strategy.x` / `simulation.strategy.o`) —
publishing an SSE event and persisting via `SessionStateWriter` after every move, until the engine
reports a terminal status or the 9-move hard cap is hit. Both strategies are implemented: `SIMPLE`
(`RandomMoveStrategy`) picks a uniformly random empty cell; `ADVANCED` (`RuleBasedMoveStrategy`) is
deterministic, following win &gt; block &gt; center &gt; corner &gt; side. `cancel()` is not
implemented yet.

## Error handling

`SessionExceptionHandler` (`@RestControllerAdvice`) extends `BaseGlobalExceptionHandler` from `tictactoe-common` — see the root README's error-code table and `docs/adr/0003-error-channels.md` / `docs/adr/0004-downstream-status-mapping.md`.

- `SessionNotFoundException` → `404 SESSION_NOT_FOUND` on `GET`/`simulate`/`events` for an unknown session. Existence is checked before ownership, so a missing session is never reported as a permissions problem.
- `NotSessionOwnerException` → `403 NOT_SESSION_OWNER` when `/simulate` is called without the cookie set on that session's `POST /sessions`, or with a different session's cookie.
- `SimulationStarter.start` → `409 SIMULATION_ALREADY_RUNNING` (session was `IN_PROGRESS`) or `409 SESSION_ALREADY_COMPLETED` (session was already terminal), both decided by a single `claimForSimulation` compare-and-swap — no separate pre-check query and no unique index, so two concurrent `/simulate` calls always produce exactly one `202` and one `409` with no race window between them.
- `GameEngineClientImpl` classifies every engine-call failure into a typed exception — never a downstream status pass-through — and retries only `EngineUnavailableException` (timeout/connection-refused/5xx), up to `ENGINE_RETRY_MAX_ATTEMPTS` with exponential backoff and jitter. A move is idempotent per `(gameId, symbol, position)`, so retrying after a timeout is safe.
- `SimulationRunner.run` guarantees a session is never left `IN_PROGRESS`: on any `RuntimeException` it persists a `FAILED` row with `error_code`/`error_message` via `SessionStateWriter.fail`, publishes a `failure` SSE event, and always completes the SSE emitter in a `finally` — persist, notify, release, in that order.
- `CorrelationIdFilter` reads/generates `X-Correlation-Id`, puts it in MDC as `traceId`, and echoes it on the response; `GameEngineClientImpl` forwards the same id to the engine, and `SimulationStarter` captures it (MDC doesn't cross `Thread.ofVirtual().start()`) so the background simulation's logs and SSE failure events carry it too.