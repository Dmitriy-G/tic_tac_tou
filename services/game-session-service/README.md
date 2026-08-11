# Game Session Service

Manages game sessions and automates gameplay by generating moves for both players, coordinating with the Game Engine Service.

## Prerequisites

- Java 21+
- Maven 3.9+
- The Game Engine Service running and reachable (see `game-engine.base-url` in `src/main/resources/application.yml`, defaults to `http://localhost:8081`)

## Running

Run these commands from this directory (`services/game-session-service`) — there is no root `pom.xml` tying the services together, so Maven must be invoked with this folder as the working directory:

```bash
cd services/game-session-service
mvn spring-boot:run
```

Or build a jar and run it directly:

```bash
mvn clean package
java -jar target/game-session-service-0.0.1-SNAPSHOT.jar
```

The service starts on port `8082` (see `src/main/resources/application.yml`).

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/sessions` | Create a new game session |
| `POST` | `/sessions/{sessionId}/simulate` | Trigger the automated simulation of a game until it concludes |
| `GET` | `/sessions/{sessionId}` | Retrieve session details, including game state and move history |

## API documentation

Swagger UI: `http://localhost:8082/swagger-ui.html`
OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Status

Skeleton only: controllers, services, and the Game Engine client are scaffolded but method bodies are not implemented yet (`UnsupportedOperationException`).
