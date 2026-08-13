# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Distributed Tic Tac Toe played automatically by microservices. Three components:

| Component | Module | Port | Responsibility |
|---|---|---|---|
| Game Engine Service | `services/tictactoe-engine` | 8081 | Board state, move validation, outcome detection. **Sole authority on game rules.** |
| Game Session Service | `services/tictactoe-session` | 8082 | Session lifecycle, automated move generation for both players, orchestration of the engine |
| Frontend | `services/tictactoe-frontend` | 5173 | Board rendering, live updates via SSE, move log, error display |

This is a home assignment. Optimise for correctness, readable structure, and test quality — not for scale.

---

## Essential Commands

```bash
# Build everything
mvn clean install
mvn clean install -DskipTests

# Tests
mvn test                                  # all
mvn test -pl services/tictactoe-engine    # one module
mvn test -Dtest=GameServiceTest
mvn test -Dtest=GameServiceTest#shouldDetectDiagonalWin

# Postgres for local (non-docker) runs — each service needs its own database reachable
docker compose up -d engine-db session-db

# Run a single service
mvn spring-boot:run -pl services/tictactoe-engine
mvn spring-boot:run -pl services/tictactoe-session

# Frontend
cd services/tictactoe-frontend
npm install
npm run dev          # Vite dev server
npm run build
npm run type-check   # tsc --noEmit
npm run lint:biome

# Full stack
docker compose up --build
```

---

## Project Structure

```
.
├── pom.xml                              # aggregator, all versions in <properties>
├── docker-compose.yml
├── README.md  CLAUDE.md
├── services/
│   ├── tictactoe-common/
│   │   ├── pom.xml  README.md          # no Dockerfile — not a runnable service
│   │   └── src/main/java/com/tictactoe/common/
│   │       ├── domain/       # GameState, Symbol, StepStatus — shared enums
│   │       └── dto/          # CreateGameRequest/Response, MoveRequest, ApplyMoveResponse, GameResponse
│   ├── tictactoe-engine/
│   │   ├── pom.xml  Dockerfile  README.md
│   │   └── src/main/java/com/tictactoe/engine/
│   │       ├── EngineApplication.java
│   │       ├── config/       # @ConfigurationProperties, beans
│   │       ├── controller/   # REST only, no logic
│   │       ├── exception/    # custom exceptions + @RestControllerAdvice
│   │       ├── repository/   # JPA entities + PostgreSQL-backed store (engine_db)
│   │       └── service/      # game rules
│   ├── tictactoe-session/
│   │   └── src/main/java/com/tictactoe/session/
│   │       ├── SessionApplication.java
│   │       ├── client/       # engine HTTP client (GameEngineClient), using tictactoe-common's DTOs
│   │       ├── config/  controller/  domain/  dto/  exception/  repository/
│   │       ├── service/      # session lifecycle, simulation runner
│   │       ├── strategy/     # MoveStrategy implementations
│   │       └── sse/          # emitter registry / event publishing
│   └── tictactoe-frontend/
│       └── src/{components,views,hooks,services,config,utils,styles}
└── docs/
    ├── adr/                             # 0001-....md, one decision per file
    ├── development/{setup,testing}/
    └── diagrams/                        # Mermaid .mmd — HLD + sequence
```

Each Maven module contains **only** `pom.xml`, `Dockerfile`, `README.md`, `src/main/java`, `src/main/resources/application.yml`, `src/test/`. Nothing else. `tictactoe-common` is the one exception — no `Dockerfile`, no `application.yml`, since it isn't a runnable service.

---

## API Contract — implement exactly as written

Paths come from the assignment spec. **Do not add a `/api/v1` prefix and do not rename them.**

### Engine

```
POST   /games                      -> 201 {gameId, board, status, nextSymbol}
POST   /games/{gameId}/move        -> 200 {gameId, board, status, nextSymbol, winner, winningLine}
GET    /games/{gameId}             -> 200 {gameId, board, status, nextSymbol, winner, winningLine, moves}
```

Move request body: `{"symbol": "X", "position": 4}` — position is `0..8`, row-major.

The move response **always returns the authoritative board.** The session service never keeps its own copy of the board.

### Session

```
POST   /sessions                   -> 201 {sessionId, status, createdAt}
POST   /sessions/{sessionId}/simulate -> 202 {sessionId, status}   (async)
GET    /sessions/{sessionId}       -> 200 {sessionId, status, board, moves[], winner, error}
GET    /sessions/{sessionId}/events-> text/event-stream (SSE)
POST   /sessions/{sessionId}/cancel-> 202
```

`sessionId` **is** the `gameId` (single UUID, one session = one game). The `GameState`/`Symbol`/`StepStatus` enums and the engine's wire DTOs (request/response records for `/games`) live in the shared `tictactoe-common` module and are used as-is by both services — see "Shared library" below. Everything else stays separate: each service keeps its own JPA entities, its own session/simulation-specific domain types (e.g. `SessionEvent`), and its own persistence — **the services still never share a database, an entity, or business logic.**

### Error response — identical shape in both services

```json
{
  "timestamp": "2026-08-12T10:15:30Z",
  "status": 409,
  "code": "CELL_OCCUPIED",
  "message": "Cell 4 is already occupied",
  "path": "/games/3f2a.../move"
}
```

### Status codes

| Code | Use |
|---|---|
| 400 | Malformed body, position out of range, unknown symbol |
| 404 | Unknown gameId / sessionId |
| 409 | Cell occupied, out-of-turn move, game already finished, simulation already running |
| 422 | Semantically invalid but well-formed request |
| 429 | Concurrent-simulation cap exceeded |
| 500 | Unexpected |
| 503 | Engine unreachable / circuit open |

Never return 500 for a client mistake. Never return 200 with an error in the body.

---

## Domain Rules (engine is the only place these live)

- Symbols are `X` and `O`. **X always moves first.**
- 8 winning lines: 3 rows, 3 columns, 2 diagonals.
- A win on the 9th cell is a **win**, not a draw. Check win before checking board-full.
- Draw = board full and no winning line.
- Statuses: `IN_PROGRESS`, `X_WON`, `O_WON`, `DRAW`.
- The engine enforces turn order itself. It does not trust the caller.
- Rejected moves: occupied cell, position outside `0..8`, symbol not `X`/`O`, symbol whose turn it isn't, any move on a finished game, unknown gameId.
- Return `winningLine` (the three indices) on a win — the UI highlights it.

## Session State Machine

```
CREATED -> IN_PROGRESS -> {X_WON | O_WON | DRAW | FAILED | CANCELLED}
```

- `/simulate` on a session already `IN_PROGRESS` -> 409. (The UI double-click case — it will happen.)
- `/simulate` on a terminal session -> 409.
- **A session must never be left in `IN_PROGRESS` after a failure.** Every exit path from the simulation loop sets a terminal status, including exceptions and thread interruption.
- Hard cap of 9 iterations in the simulation loop as a safety net, independent of the engine's status response.

## Move Generation

- `MoveStrategy` interface with a `selectMove(board, symbol)` method; implementations `RandomMoveStrategy` and `RuleBasedMoveStrategy` (win > block > center > corner > side).
- Strategy is resolved per symbol from config, so X and O can differ.
- `Random` is **injected**, never `new Random()` inline. Tests supply a seeded instance.
- Never pick from occupied cells; "no empty cells" is a normal loop exit, not an exception.
- Configurable delay between moves (`simulation.move-delay-ms`, default 500) so the UI shows progression. **Tests set it to 0.**

---

## Shared library (`tictactoe-common`)

A third Maven module, `services/tictactoe-common`, holds only what is byte-for-byte identical
because it *is* the engine's HTTP wire contract: the `GameState`, `Symbol`, `StepStatus` enums and
the `/games` request/response records (`CreateGameRequest`, `CreateGameResponse`, `MoveRequest`,
`MoveResponse`, `GameResponse`). Both `tictactoe-engine` and `tictactoe-session` depend on it.

Rules for this module:

- Plain Java only — no Spring, no persistence, no business logic, no controllers. It must never
  depend on either service.
- It is not a runnable service: no `Dockerfile`, no `docker-compose` entry, no port.
- Nothing else moves here. Session-only types (`SessionEvent`, `SessionResponse`), JPA entities,
  and mappers stay private to their owning service, per the Storage section below. If a class
  isn't part of the engine's wire contract, it doesn't belong in `tictactoe-common` — resist
  growing it into a dumping ground for "shared-ish" code.

---

## Code Standards (Java)

- Java 21 — records for DTOs, sealed interfaces and pattern matching where they fit, enhanced switch.
- Spring Boot 3.3.0, Maven multi-module. Parent = `spring-boot-starter-parent`; every version pinned in the root `<properties>`.
- **Constructor injection only.** No `@Autowired` on fields.
- Lombok: `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`, `@Value`. Do not put `@Data` on domain objects.
- DTOs are records and are separate from domain objects. Map explicitly in a `mapper/` class — no leaking domain types out of the controller.
- Controllers hold no logic: validate, delegate, map, return.
- `@Valid` + `@Min`/`@Max`/`@NotNull` on every request DTO.
- One `@RestControllerAdvice` per service; custom exceptions carry a `code` used in the error body.
- All config in `application.yml`, env-overridable (`${ENGINE_BASE_URL:http://localhost:8081}`). Nothing hardcoded, ever. Typed via `@ConfigurationProperties`.
- Naming: `*Controller`, `*Service`, `*Repository`, `*Mapper`, `*Application`.
- Default IntelliJ Java formatting.

### Storage

Each service owns a dedicated PostgreSQL database — `engine_db` for the engine, `session_db` for the session service. **The two services never share a database or a schema**; each has its own JDBC datasource, its own Flyway migration history, and its own JPA entities living in that service's `repository/` package (entities are private to the repository layer — domain classes stay plain and DB-agnostic, mapped explicitly in the `*Repository` implementation, the same way DTOs are mapped in `mapper/`).

- **Schema is owned by Flyway**, not Hibernate: `spring.jpa.hibernate.ddl-auto` is `validate`, never `update` or `create`. Migrations live in `src/main/resources/db/migration` per service.
- `engine_db.games (id UUID, board VARCHAR(9), state VARCHAR(20))` — the 9-cell board is encoded as a 9-character string (`X`/`O`/`_` for empty); `winner` is derived from `state`, not stored separately.
- `session_db.sessions (id UUID)` — an identity/anchor row only; `sessionId` **is** `gameId` so no separate column is needed, and the board/move history are deliberately not duplicated here (see anti-patterns).
- `session_db.simulations (id UUID, session_id UUID FK -> sessions, errors_count INT, started_at TIMESTAMP, finished_at TIMESTAMP, status VARCHAR(20))` — one row per `/simulate` run, the audit trail of run outcomes.
- Connection pool: explicit connect/read behaviour via the datasource properties in `application.yml`, env-overridable (`ENGINE_DB_URL`/`SESSION_DB_URL` + `*_USERNAME`/`*_PASSWORD`), same pattern as `ENGINE_BASE_URL`.
- Tests run against H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`) via `application-test.yml`, with the same Flyway migrations applied — not Testcontainers (out of scope, see below).
- A `ConcurrentHashMap`-backed read-modify-write guard is still required wherever mutable in-flight state genuinely can't live in the database without duplicating another service's data (e.g. a session's live status/move history while a simulation is running) — mutate through `compute()` or a per-key lock so two concurrent moves on the same game produce exactly one success and one 409.

### Engine HTTP client (session service)

- `RestClient` (or `WebClient`) with **explicit connect and read timeouts** — the defaults are effectively infinite. This is the most common failure in this task.
- Spring Retry / Resilience4j: bounded retries with backoff, plus a circuit breaker. Retries are safe because the move endpoint is idempotent per `(gameId, symbol, position)`.
- Engine down at session creation -> fail fast with 503, do not create a half-session.
- Engine 409 mid-simulation -> re-read the authoritative board from the response and pick a new cell; after N failures mark the session `FAILED`.
- Malformed or unexpected engine response -> defensive parsing, never an NPE propagated to the caller.

### Observability

- `spring-boot-starter-actuator` in both services; the session service's readiness reflects engine reachability.
- Correlation id = `sessionId`, sent as `X-Correlation-Id` and put in MDC. Every log line for a simulation must be greppable by it.
- Structured JSON logging via `logstash-logback-encoder`.
- Log every move at INFO with sessionId, move number, symbol, position, resulting status.

---

## Testing Rules

- JUnit 5 + Mockito, `@SpringBootTest`, MockMvc for the web layer, `MockRestServiceServer`/WireMock for the engine from the session side.
- **Tests must be deterministic.** No `Thread.sleep`, no wall-clock assertions, no reliance on iteration order. Seed the `Random`, set move delay to 0, inject the strategy. A flaky test here is a failed assignment.
- `application-test.yml` disables anything external so tests run standalone.

Coverage that must exist:

- All 8 win lines, plus win-on-9th-cell beating draw, plus draw.
- Every rejection path with its exact status code and error `code`.
- Turn-order enforcement, including a direct out-of-turn POST to the engine.
- Session service against a mocked engine: success, 404, 409, 500, timeout, garbage body.
- Full integration flow: create session -> simulate -> poll to a terminal status; assert history length is 5–9 and that replaying the history reproduces the final board.
- Concurrency: parallel moves to the same cell -> exactly one 200 and one 409; N parallel simulations all reach a terminal state.

---

## Frontend

- React 19 + TypeScript strict + Vite. Biome for lint and format (not ESLint/Prettier).
- `@tanstack/react-query` for REST; native `EventSource` for the SSE stream.
- PascalCase component files, `use*` hooks, camelCase service files (`gameApiService.ts`). Named exports for hooks/utils, default export for pages.
- Required behaviour:
    - Start button disabled while a simulation is running.
    - Board renders empty before start; winning line highlighted on completion.
    - Move log with move numbers.
    - **SSE fallback**: if the stream errors, fall back to polling `GET /sessions/{id}`.
    - Page refresh mid-simulation rehydrates from `GET /sessions/{id}` — never show an empty board for a running game.
    - Late subscriber gets the backlog of moves then live events.
    - Error banner for: session creation failed, simulation failed, stream lost, backend unreachable.
- No `localStorage`/`sessionStorage`.

---

## Docker

Multi-stage per service, mirroring the reference project:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml ./
COPY services/ services/
RUN mvn clean package -pl services/tictactoe-engine -am \
    --batch-mode --no-transfer-progress -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S -G appgroup appuser
WORKDIR /app
COPY --from=builder --chown=appuser:appgroup /build/services/tictactoe-engine/target/*.jar app.jar
USER appuser
ENTRYPOINT ["java","-jar","app.jar"]
```

Non-root user, JRE-only runtime layer. `docker compose up` must bring up all five (two `postgres:16-alpine` databases plus the three services) with one command; the engine and session services `depends_on` their respective database with a `service_healthy` condition (`pg_isready`) so they never start against a database that isn't ready yet.

---

## Git Conventions

- Branches: `<type>/<kebab-description>` — `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`.
- Conventional Commits: `<type>(<scope>): <summary>`. Scopes: `engine`, `session`, `frontend`, `docs`, `ci`.
- PR description: Summary / Changes / Testing / Breaking Changes.
- Small, focused commits — one logical change each.

## Review Checklist

- [ ] Edge cases handled; error handling explicit
- [ ] No race conditions on shared state
- [ ] Input validated with `@Valid`
- [ ] Tests cover happy path **and** error scenarios, and are deterministic
- [ ] Conventions followed; no dead code or unused imports
- [ ] New config in `application.yml`, documented in the README
- [ ] No secrets committed

---

## Out of Scope — do not add these

Authentication/authorisation, API gateway, Eureka, Kafka or any message broker, MongoDB/Cassandra/any NoSQL store, GraphQL, CDC, Testcontainers, LLM-driven move strategies, batch simulation of multiple games per request, multi-tenancy.

Each of these belongs in `docs/adr/` as a considered-and-rejected alternative with rationale — that scores better than implementing it. REST is a deliberate choice for this scope, not an omission. PostgreSQL (one instance per service, via Flyway-managed schemas) **is** in scope — see Storage above; don't reintroduce in-memory maps as the system of record for `games`, `sessions`, or `simulations`.

"API gateway" above means a dedicated routing service/container — still out of scope. Putting the browser-facing API on the same origin as the UI does **not** need one: the frontend's own nginx (already serving the built assets) reverse-proxies `/api/*` to the session service, which is what removes CORS from application code entirely. See `docs/adr/0005-edge-routing-no-gateway.md`.

## Anti-patterns to avoid

- HTTP client without explicit timeouts
- Field injection
- Business logic in controllers
- The session service maintaining its own copy of the board
- `new Random()` inside a strategy
- Storing move history in both services
- Catching `Exception` and returning 200
- Unbounded thread pool for simulations — use a bounded executor and reject with 429
- `Thread.sleep` in tests

---

## When Making Changes

1. Rules changes go in the engine's `service/`, never in the session service.
2. Any new endpoint gets: a DTO record, validation, an error path, a MockMvc test, and a README line.
3. Any new config key gets a default in `application.yml` and a mention in the service README.
4. Update `docs/diagrams/` if a call flow changes.
5. Run `mvn test` and `npm run type-check` before considering a change done.
