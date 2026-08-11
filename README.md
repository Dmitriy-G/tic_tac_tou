# Distributed Tic Tac Toe

A distributed Tic Tac Toe application in which the game is played automatically by microservices, with a UI that watches the game unfold.

## Components

| Component | Path | Stack | Port |
| --- | --- | --- | --- |
| Game Engine Service | `services/game-engine-service` | Java, Spring Boot | 8081 |
| Game Session Service | `services/game-session-service` | Java, Spring Boot | 8082 |
| UI | `client` | React, TypeScript | 5173 (dev) |

- **Game Engine Service** — owns the board, validates moves, and determines the game outcome (in progress, win, or draw).
- **Game Session Service** — creates and manages game sessions, automates moves for both players, and coordinates with the Game Engine Service.
- **UI** — starts a simulation and renders the board, status, and move history as the microservices play against each other.

## Running each service

Each service is a standalone project; see its own README for details.

### Game Engine Service

```bash
cd services/game-engine-service
mvn spring-boot:run
```

### Game Session Service

```bash
cd services/game-session-service
mvn spring-boot:run
```

### UI

```bash
cd client
npm install
npm run dev
```

## Structure

```
tic_tac_tou_ui/
  client/                          # React + TypeScript UI
  services/
    game-engine-service/           # Java + Spring Boot
    game-session-service/          # Java + Spring Boot
```
