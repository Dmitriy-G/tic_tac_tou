do git# 0002: Session ownership tokens and a shared internal secret, no user identity

## Status

Accepted.

Depends on [0001](0001-edge-routing-no-gateway.md) — the same-origin serving decided there is a
precondition for the cookie transport chosen below.

Supersedes the "Authentication/authorisation" entry in the root `CLAUDE.md` **Out of Scope**
section, which must be amended: authentication stays out of scope, session-scoped authorization
does not.

## Context

The assignment specification does not mention security. It also describes no user, login, account,
or ownership concept — the API surface is five endpoints operating on server-generated UUIDs. Taken
literally, there is nothing to authenticate.

That reading is incomplete. The design notes for this project raised the question and left it open
("Access rules for simulation?", "How many observers for one simulation?"). Those are authorization
questions and they have answers without any user model.

### Two distinct questions

- **Authentication** — *who is calling?* Requires a user model. There is none, and inventing one is
  out of scope.
- **Authorization** — *may this caller do this to this session?* Answerable without user identity,
  because sessions already have an identity of their own.

Only the second is in scope.

### Threat model

Assessed against the current codebase, ordered by exploitability:

1. **Direct engine manipulation.** `docker-compose.yml` publishes `8081:8081`. Any process that can
   reach the host can `POST /games/{gameId}/move` against a game that is mid-simulation. This is not
   theoretical: it is exactly the concurrent-move case `SimulationProgress`'s error budget absorbs,
   and an outsider can drive a session to `FAILED` by exhausting its eleven retries. `GameController`
   also accepts a caller-supplied `gameId` on `POST /games`, so an outsider can pre-create or
   collide with a game id.
2. **Session hijacking.** Any party holding a `sessionId` can `POST /sessions/{id}/simulate`.
   Session ids are UUIDv4 and not enumerable, but they travel in URLs and are visible to anyone the
   creator shares one with — including parties who should only be able to *watch*.
3. **Database ports on the host.** `5433:5432` and `5434:5432` are published with credentials that
   are literals in the compose file. Convenient for inspection, and a wider hole than the API.
4. **Cross-site request forgery.** With the API on the same origin as the UI (0001), a
   cookie-carried credential is submitted automatically by the browser on cross-site form posts
   unless deliberately scoped.

Not in the threat model: multi-tenancy, data confidentiality (a tic-tac-toe board is not
sensitive), audit or compliance, denial of service beyond the per-session error budget.

## Decision

Four measures, ordered by value per line of code.

### 1. Do not publish the engine or database ports

```yaml
  tictactoe-engine:
    expose:
      - "8081"        # was: ports: ["8081:8081"]
```

Same for `engine-db` and `session-db`. The session service continues to reach the engine over the
Compose network by DNS; `ENGINE_BASE_URL` is unchanged. This eliminates threats (1) and (3) outright
and is the highest-value change here — configuration only, no code.

Two consequences to handle rather than accept silently:

- **The engine's Swagger UI is no longer reachable from the host.**
  `springdoc-openapi-starter-webmvc-ui` is a dependency of both services. Document
  `docker compose exec`, or a debug profile that republishes the port, in the README.
- **`tictactoe-session` currently depends on the engine with `condition: service_started`.** The
  engine has no healthcheck. Add one on `/actuator/health` and upgrade the condition to
  `service_healthy`, or a slow engine start surfaces as `ENGINE_UNAVAILABLE` on the first
  simulation.

### 2. Shared secret on the internal boundary

The engine accepts requests only from the session service, verified by a `OncePerRequestFilter`
comparing an `X-Internal-Token` header against a configured value. Model it on the existing
`CorrelationIdFilter` in `com.tictactoe.engine.config`.

```java
if (!MessageDigest.isEqual(expected.getBytes(UTF_8),
                           String.valueOf(req.getHeader(HEADER)).getBytes(UTF_8))) {
    // 401 UNAUTHORIZED, standard ErrorResponse body
}
```

- `MessageDigest.isEqual`, not `String.equals` — constant-time comparison, no timing side channel.
- The value comes from an environment variable in both services (`INTERNAL_TOKEN`), never a
  committed default. Startup fails fast if absent or blank.
- `GameEngineClientImpl` supplies it as a header alongside the correlation id it already sends.
- `/actuator/**` is exempt, or the healthcheck added in measure (1) fails.

This is defence in depth behind measure (1), and it is the boundary that survives if the engine is
ever exposed again for any reason.

### 3. Session ownership token

`POST /sessions` returns an opaque high-entropy token alongside the session id. The session row
stores only a SHA-256 hash of it. `sessions` currently holds nothing but `id UUID PRIMARY KEY`, and
the session service is at `V3`, so:

```sql
-- V4__session_owner_token.sql
ALTER TABLE sessions ADD COLUMN owner_token_hash VARCHAR(64) NOT NULL;
```

Access rules — this is the answer to the open "access rules for simulation" question:

| Endpoint | Rule |
|---|---|
| `POST /sessions` | open — creates the credential |
| `POST /sessions/{id}/simulate` | owner only → **403 `NOT_SESSION_OWNER`** |
| `GET /sessions/{id}` | open |
| `GET /sessions/{id}/events` | open |

**Writes are owner-only; reads are open.** This is the "one owner, many observers" model the design
notes were reaching for: holding a session id lets you watch a game, not steer it. It also matches
what `SseEmitterRegistry` already supports — multiple subscribers per session.

403 rather than 404-to-prevent-enumeration: session ids are unguessable and reads are open anyway,
so hiding existence buys nothing and obscures the failure for a legitimate caller who lost their
token.

### 4. Transport: an `HttpOnly` cookie, not a bearer header

```
Set-Cookie: tictactoe_session_owner=<token>; HttpOnly; SameSite=Strict; Path=/api
```

**`EventSource` cannot set custom headers.** There is no API for it. An
`Authorization: Bearer …` scheme would authenticate every endpoint except the SSE stream — the one
that matters most here.

A cookie is sent automatically by both `fetch` (default `credentials: 'same-origin'`) and
`EventSource` on same-origin requests, which the reverse proxy in
[0001](0001-edge-routing-no-gateway.md) guarantees. **The frontend therefore needs no changes at
all** — `gameApiService.ts` keeps its relative `/api/...` URLs and the browser attaches the
credential itself. The two ADRs compose: 0001 was taken to remove CORS, and it incidentally makes
the only workable SSE credential scheme available for free.

`SameSite=Strict` closes threat (4) for the POST endpoints in the same stroke. `Secure` is set from
configuration, off for plain-HTTP local runs.

### Error codes

`UNAUTHORIZED(401)` and `NOT_SESSION_OWNER(403)` are added to the generic block of the shared
`ErrorCode` enum and returned through the existing `ErrorResponse` shape, with the same `traceId`.
Security failures are not a separate error channel.

## Rejected alternatives

**Spring Security with JWT and a login flow.** Requires a user store, login endpoint, token
refresh, and signing-key management — none requested, all of it code a reviewer must read to
evaluate a tic-tac-toe simulator. Separately, adding `spring-boot-starter-security` locks every
endpoint behind HTTP Basic by default, including Actuator and the springdoc UI; the resulting
configuration work exceeds the four measures above combined.

**Authentication at nginx (Basic auth).** Three lines, but it protects the *service*, not the
*session*. It cannot express "the owner may simulate, an observer may only watch," because nginx has
no notion of session ownership. Ownership is domain logic and belongs in the session service. This
is also why 0001's "no edge authentication" is a decision rather than an omission.

**Bearer token in a query parameter** (`/events?token=…`). The usual workaround for `EventSource`'s
header limitation. Rejected because tokens then appear in access logs, browser history, and
`Referer` headers. The cookie achieves the same result without leaking.

**mTLS between services.** Correct for the internal boundary and disproportionate here —
certificate generation and rotation for a two-container Compose file.

## Consequences

- The engine and both databases are no longer reachable from the host. Manual API exploration goes
  through the session service, `docker compose exec`, or a documented debug override.
- `INTERNAL_TOKEN` must be set for the system to start. Deliberate: a default that works out of the
  box is a default that ships to production. `application-test.yml` in both services needs a fixed
  value, and the Compose file needs it on both services.
- Clients must retain the owner token to control a session. The browser gets it in a cookie
  automatically; a `curl` user must pass `-c`/`-b`. Document this in the README's API walkthrough,
  or the first person to try `curl` gets a 403 and assumes it is a bug.
- The token is returned exactly once, at creation, and stored only as a hash. It cannot be
  recovered. Correct for this scope; a real system needs rotation or recovery.
- `SessionResponse` gains an `ownerToken` field populated only on creation and null everywhere else
  — a slightly awkward record. The alternative (a separate creation DTO) is weighed in the
  implementation plan.
- No user identity exists anywhere. `GET /sessions/{id}` cannot answer "who created this" and is not
  expected to.
- Tests must exercise 401 and 403 alongside the existing error-code assertions in
  `GameControllerErrorHandlingTest` and `SessionControllerErrorHandlingTest`.

## Revisit when

- **Users appear.** Any concept of an account replaces ownership tokens with real authentication,
  and this ADR is superseded rather than amended.
- **A second client** (mobile, CLI) needs to control sessions — cookies stop being the natural
  transport and a bearer scheme plus an SSE polyfill becomes worth the code.
- **Sessions become long-lived or shareable by link**, at which point token expiry and revocation
  are needed; there is deliberately none today.
- **A third service** joins, making a shared internal secret an N-way secret and pushing toward
  mTLS or per-service credentials.
- **Deployment beyond a single Compose network**, where the network-topology assumption behind
  measure (1) no longer holds and measure (2) becomes load-bearing rather than defence in depth.
