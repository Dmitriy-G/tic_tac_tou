# Game Engine Service

Owns the Tic Tac Toe board state: validates moves and determines the game outcome (in progress, win, or draw).

## Prerequisites

- Java 21+
- Maven 3.9+

## Running

Run these commands from this directory (`services/game-engine-service`) — there is no root `pom.xml` tying the services together, so Maven must be invoked with this folder as the working directory:

```bash
cd services/game-engine-service
mvn spring-boot:run
```

Or build a jar and run it directly:

```bash
mvn clean package
java -jar target/game-engine-service-0.0.1-SNAPSHOT.jar
```

The service starts on port `8081` (see `src/main/resources/application.yml`).

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/games/{gameId}/move` | Validate and apply a move, return the updated game state and status |
| `GET` | `/games/{gameId}` | Retrieve the current board and status for a game |

## Status

Skeleton only: controllers, services, and models are scaffolded but method bodies are not implemented yet (`UnsupportedOperationException`).
