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
- `simulations (id UUID, session_id UUID FK -> sessions, errors_count INT, started_at TIMESTAMP, finished_at TIMESTAMP, status VARCHAR)` — one row per `/simulate` run, the durable audit trail of run outcomes. `SessionServiceImpl` writes a row when a simulation starts and updates it with the terminal status when the run ends.

A session's *live*, in-flight `status`/move history while a simulation is actually running is process-local state, kept in memory by `PostgresSessionRepository` alongside the persisted identity row — it is not written to `session_db` (see the class Javadoc). On a fresh JVM (e.g. after a restart) `GET /sessions/{id}` reconstructs status from the latest `simulations` row instead.

## Status

The SSE pipeline (session → events → client) works end-to-end today, but `simulate()` drives a couple of **scripted mock move sequences** rather than real gameplay — the real simulation would delegate move generation and rule enforcement to `GameEngineClient`, which (along with `cancel()`, the `strategy/` implementations, and the engine client's timeouts/retries) is still a skeleton (`UnsupportedOperationException`). `dto/` is a placeholder package, not yet populated.