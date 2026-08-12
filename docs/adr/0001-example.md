# 0001: Plain REST, not a message broker or service registry

## Status

Accepted. The persistence decision originally recorded here (in-memory `ConcurrentHashMap`, no
database) is **superseded by [0002](0002-postgresql-per-service.md)** — each service now has its
own PostgreSQL database. The rest of this decision (no broker, no gateway, no service discovery)
still stands.

## Context

This is a home assignment scoped to two Spring Boot services and a frontend, evaluated on
correctness, readable structure, and test quality — not on scale or production operability.
Reasonable-looking additions like a message broker for session/engine coordination or a service
registry (Eureka) exist as options.

## Decision

Session-to-engine communication is synchronous REST (`RestClient`/`WebClient`) with retries and a
circuit breaker, not an async broker (Kafka or similar). There is no API gateway and no service
discovery (Eureka) — both services are configured with each other's base URL directly.

## Consequences

- No multi-instance deployment story: a second `tictactoe-session` instance would not coordinate
  with the first beyond what the shared `session_db` gives it for free. Out of scope per the
  assignment.
- Simpler to read, run, and test than the alternatives — no broker to stand up locally.

See the root `CLAUDE.md`'s "Out of Scope" section for the full list of deliberately rejected
additions (auth, API gateway, Kafka, GraphQL, CDC, Testcontainers, LLM-driven strategies,
multi-tenancy).
