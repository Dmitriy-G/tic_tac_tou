# Game Engine Service (`tictactoe-engine`)

Owns the Tic Tac Toe board state: validates moves and determines the game outcome (in progress, win, or draw). Sole authority on game rules — the session service never keeps its own copy of the board.

## Prerequisites

- Java 21+
- Maven 3.9+

## Running

From the repository root (this module is part of the aggregator `pom.xml`):

```bash
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

## Status

Skeleton only: controllers, services, and domain classes are scaffolded but method bodies are not implemented yet (`UnsupportedOperationException`). `config/`, `repository/` hold placeholder/in-memory-map code not yet wired into the service layer.