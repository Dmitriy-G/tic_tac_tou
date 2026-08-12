# 0002: One PostgreSQL database per service, schema owned by Flyway

## Status

Accepted. Supersedes the persistence portion of [0001](0001-example.md).

## Context

State was originally kept in a `ConcurrentHashMap`-backed in-memory repository per service (see
0001): simple, but it does not survive a restart and gives no durable audit trail of what a
session actually did. The next iteration of the assignment calls for durable storage, still
without pulling in anything the assignment considers out of scope (no shared database between
services, no NoSQL store, no Testcontainers for tests).

## Decision

- **Two separate PostgreSQL 16 instances**, one per service (`engine_db`, `session_db`), each with
  its own container in `docker-compose.yml`, its own credentials, and its own JDBC datasource. The
  services never share a database or a schema — that would recreate the coupling the two-service
  split is meant to avoid.
- **Flyway owns the schema**, not Hibernate: `spring.jpa.hibernate.ddl-auto` is `validate` in both
  services. Every schema change is a new versioned migration under `src/main/resources/db/migration`.
- **`engine_db.games (id, board, state)`** stores exactly the engine's authoritative state. The
  board is encoded as a 9-character string; `winner` is derived from `state` on read rather than
  stored, since it is fully determined by it.
- **`session_db.sessions (id)`** is deliberately a bare identity/anchor row. It does not store
  `gameId` (identical to `id` per the API contract — `sessionId` **is** `gameId`), the board (the
  engine's copy is authoritative and is never duplicated — an explicit anti-pattern in the root
  `CLAUDE.md`), or move history (same reasoning — the engine already owns move history).
- **`session_db.simulations (id, session_id, errors_count, started_at, finished_at, status)`**
  captures the durable outcome of each `/simulate` run — an audit trail distinct from a session's
  live, in-flight status. `SessionServiceImpl` writes a row when a run starts and updates it with
  the terminal status when the run ends or fails.
- A session's *live* status and move history while a simulation is actively running are
  necessarily process-local (there is nowhere in the schema above for them, by design) and are
  held in memory by `PostgresSessionRepository` alongside the persisted identity row. This is a
  narrower, more deliberate version of the same trade-off 0001 made for everything: it is scoped
  to genuinely transient in-flight state, not used as the system of record for anything durable.
- Tests run against H2 in PostgreSQL compatibility mode (`MODE=PostgreSQL`), with the same Flyway
  migrations applied, rather than Testcontainers (still out of scope — see `CLAUDE.md`).

## Consequences

- `games`, `sessions`, and `simulations` survive a service restart; a session's live in-flight
  status/move history does not — after a restart mid-simulation, `GET /sessions/{id}` falls back
  to the latest `simulations` row's status instead of the live in-memory value.
- Both services now require a reachable PostgreSQL instance to start (`docker compose up -d
  engine-db session-db` for local, non-container runs) — there is no more zero-dependency `mvn
  spring-boot:run`. This is judged worth it for a durable audit trail and is still a single
  `docker compose up` away.
- H2-in-PostgreSQL-mode is not a perfect stand-in for real PostgreSQL (e.g. some PostgreSQL-only
  SQL features would not be caught by the test suite). Given the migrations here are simple
  (`UUID`/`VARCHAR`/`INTEGER`/`TIMESTAMP` columns, one foreign key, one index), this is an
  acceptable gap for the assignment's scope rather than pulling in Testcontainers.