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

`GameServiceImpl` is fully implemented: it enforces turn order (`X` moves first), rejects out-of-range positions, occupied cells, out-of-turn moves and moves on a finished game, detects all 8 winning lines (including a win on the 9th cell beating a draw), and persists through `GameRepository` (Postgres-backed, `PostgresGameRepository`). Moves against the same `gameId` are serialized with a per-game lock so concurrent requests can't corrupt the board. `POST /games` accepts an optional `{"gameId": "..."}` body so the session service can force `gameId == sessionId`; when omitted, the engine generates one. `GameExceptionHandler` (`@RestControllerAdvice`) maps `GameNotFoundException` to `404` and `InvalidMoveException` to `400`/`409` (per its `code`) using the error shape from the root `CLAUDE.md`. `config/` still holds no beans — nothing needed one yet.