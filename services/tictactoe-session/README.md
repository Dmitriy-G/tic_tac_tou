# Game Session Service (`tictactoe-session`)

Manages game session lifecycle and automates gameplay by generating moves for both players, coordinating with the Game Engine Service. The engine remains the sole authority on game rules — this service never keeps its own copy of the board.

## Prerequisites

- Java 21+
- Maven 3.9+
- The Game Engine Service running and reachable (see `game-engine.base-url` in `src/main/resources/application.yml`, defaults to `http://localhost:8081`, overridable via `ENGINE_BASE_URL`)

## Running

From the repository root (this module is part of the aggregator `pom.xml`):

```bash
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

## Status

The SSE pipeline (session → events → client) works end-to-end today, but `simulate()` drives a couple of **scripted mock move sequences** rather than real gameplay — the real simulation would delegate move generation and rule enforcement to `GameEngineClient`, which (along with `cancel()`, the `strategy/` implementations, and the engine client's timeouts/retries) is still a skeleton (`UnsupportedOperationException`). `config/` and `dto/` are placeholder packages, not yet populated.