# Game Engine Service (`tictactoe-engine`)

Owns the Tic Tac Toe board state: validates moves and determines the game outcome (in progress, win, or draw). Sole authority on game rules — the session service never keeps its own copy of the board.

## Prerequisites

- Java 21+
- Maven 3.9+
- A PostgreSQL 16 database reachable at startup — `docker compose up -d engine-db` from the repository root, or point `ENGINE_DB_URL` at your own instance. Schema is created by Flyway on boot (`src/main/resources/db/migration`); there is no `ddl-auto` fallback.

## Configuration

| Env var | Default | Purpose |
| --- | --- | --- |
| `ENGINE_DB_URL` | `jdbc:postgresql://localhost:5433/engine_db` | JDBC URL for `engine_db` |
| `ENGINE_DB_USERNAME` | `engine` | Database username |
| `ENGINE_DB_PASSWORD` | `engine` | Database password |

## Running

From the repository root (this module is part of the aggregator `pom.xml`):

```bash
docker compose up -d engine-db
mvn spring-boot:run -pl services/tictactoe-engine
```

Or build a jar and run it directly:

```bash
mvn clean package -pl services/tictactoe-engine -am
java -jar services/tictactoe-engine/target/tictactoe-engine-0.0.1-SNAPSHOT.jar
```

The service starts on port `8081` (see `src/main/resources/application.yml`).

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/games` | Create a new game |
| `POST` | `/games/{gameId}/move` | Validate and apply a move, return the updated game state and status |
| `GET` | `/games/{gameId}` | Retrieve the current board, status, and move history for a game |

See the root `CLAUDE.md` for the full request/response contract and status code table.

## API documentation

Swagger UI: `http://localhost:8081/swagger-ui.html`
OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Storage

The `games` table lives in its own `engine_db` PostgreSQL database (never shared with the session service). Schema is managed entirely by Flyway (`src/main/resources/db/migration/V1__create_games_table.sql`); Hibernate is set to `ddl-auto: validate` and never creates or alters the schema itself. The 9-cell board is stored as a 9-character string (`X`/`O`/`_` for empty); `winner` is derived from `state` on read rather than stored as its own column.

## Status

`GameService` is orchestration only: it loads the game through `GameStore`, delegates validation to `MoveValidator` (which in turn asks `TurnPolicy` whose move it is), delegates outcome resolution to `GameOutcomeEvaluator` (win checked before fullness, so a win on the 9th cell beats a draw), and persists through `GameStore`. `WinningLines` (`config/`) parses and validates `game.winning-lines` at startup, failing context startup with `IllegalStateException` on a malformed property instead of blowing up mid-game. `POST /games` accepts an optional `{"gameId": "..."}` body so the session service can force `gameId == sessionId`; when omitted, the engine generates one.

## Error handling

`GameExceptionHandler` (`@RestControllerAdvice`) extends `BaseGlobalExceptionHandler` from `tictactoe-common` — see the root README's error-code table and `docs/adr/0003-error-channels.md` / `docs/adr/0004-downstream-status-mapping.md`. Rule outcomes (`StepStatus` in a `200` `MoveResponse` — occupied cell, out-of-turn, finished game, etc.) never go through this handler; only faults do:

- `GameNotFoundException` (extends `NotFoundException`) → `404 GAME_NOT_FOUND`.
- A non-UUID `gameId` path variable → `400 INVALID_GAME_ID` (caught at `GameStore`, the point where `UUID.fromString` actually throws).
- `MoveRequest` validation (`@NotNull symbol`, `@Min(0) @Max(8) position`) → `400 VALIDATION_ERROR` with a `fieldErrors` entry per failed constraint.
- Everything else inherited from `BaseGlobalExceptionHandler` (malformed JSON, wrong HTTP method, repository failure, unexpected exception) with no engine-specific override needed.
- `CorrelationIdFilter` reads/generates `X-Correlation-Id`, puts it in MDC as `traceId`, and echoes it on the response; every error body's `traceId` field comes from there.