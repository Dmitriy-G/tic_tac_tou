# 0001: In-memory storage and plain REST, not a database or message broker

## Status

Accepted

## Context

This is a home assignment scoped to two Spring Boot services and a frontend, evaluated on
correctness, readable structure, and test quality — not on scale, durability across restarts, or
production operability. Reasonable-looking additions like a real database, a message broker for
session/engine coordination, or a service registry (Eureka) exist as options.

## Decision

Both services hold state in a `ConcurrentHashMap`-backed in-memory repository. Session-to-engine
communication is synchronous REST (`RestClient`/`WebClient`) with retries and a circuit breaker,
not an async broker (Kafka or similar). There is no persistence dependency (H2/Postgres/Mongo), no
API gateway, and no service discovery (Eureka) — both services are configured with each other's
base URL directly.

## Consequences

- State does not survive a service restart; this is acceptable for the assignment's scope.
- No multi-instance deployment story: a second `tictactoe-session` instance would not see sessions
  created against the first. Out of scope per the assignment.
- Simpler to read, run, and test than the alternatives — no Testcontainers, no broker to stand up
  locally, `docker compose up` is enough.

See the root `CLAUDE.md`'s "Out of Scope" section for the full list of deliberately rejected
additions (auth, API gateway, Kafka, a real database, GraphQL, CDC, Testcontainers, LLM-driven
strategies, multi-tenancy).
