# 0001: Reverse proxy at the frontend, no API gateway and no service registry

## Status

Accepted.

Related: [0002](0002-security-model.md) depends on the same-origin serving decided here — it is a
precondition for the only credential scheme that works with `EventSource`.

## Context

The assignment lists service discovery and an API gateway as optional enhancements:

> **Service Discovery / API Gateway:** Integrate solutions like Spring Cloud Gateway or Eureka to
> simulate a production microservices environment.

Three concerns push in that direction:

- **CORS is hardcoded in application code.** `SessionController` carries
  `@CrossOrigin(origins = "http://localhost:5173")`, and the frontend container is configured with
  `VITE_SESSION_SERVICE_URL: http://localhost:8082`. The browser reaches the session service by
  published host port, so the value is correct only for a Compose deployment on the developer's own
  machine.
- **Two origins.** The frontend is served from one port and calls an API on another, which is what
  makes CORS necessary in the first place.
- **No single entry point.** A reviewer running the project has to know three ports.

None of these is a discovery problem. The topology is fixed at two backend services with one
instance each, and inter-service addressing is already solved: `ENGINE_BASE_URL` is externalised
and resolved through Compose's DNS (`http://tictactoe-engine:8081`).

### Prior art in the task authors' own codebase

The reference implementation (`openframe-oss-lib`, `openframe-oss-tenant`) was examined directly.
It runs nine services and:

- **Uses Spring Cloud Gateway** (`spring-cloud-starter-gateway`, WebFlux) as a real service on port
  8100.
- **Uses no service registry at all.** Gateway routes point at hardcoded Kubernetes DNS names
  (`http://openframe-api.tenant.svc.cluster.local:8090`). Kubernetes Service DNS *is* the discovery
  mechanism. A single `@EnableDiscoveryClient` annotation survives on the authorization server with
  no registry behind it — a leftover with no effect.
- Earns the gateway on cross-cutting concerns that do not exist in this project: OAuth2/JWT
  termination, API-key authentication, Redis-backed rate limiting, traffic metrics, WebSocket
  proxying, and CORS across nine backends and an SPA.

That distinction is the crux of this decision. Discovery is not what a gateway is for at this
scale; cross-cutting edge concerns are. This project has exactly one such concern — CORS.

### The SSE hazard

The reference gateway configuration carries an unresolved comment:

> gateway closes SSE connections at `response-timeout` (default 30s). This was previously raised to
> 5m to keep SSE streams open, but is intentionally set to 20s. TODO: find better solution how to
> handle it. It should be possible to configure for only SSE routings.

`GET /sessions/{sessionId}/events` is a long-lived SSE stream and would sit behind exactly this
timeout. The cause is that `spring.cloud.gateway.httpclient.response-timeout` is global: normal REST
routes want it short so a wedged upstream fails fast, and SSE wants it long or absent. A single
value cannot serve both, so raising it for SSE degrades failure detection everywhere else.

## Decision

**No API gateway and no service registry.** Edge routing is handled by the nginx instance that
already serves the frontend build.

1. **Service-to-service addressing stays as-is.** `ENGINE_BASE_URL` resolved via Compose DNS. This
   is the same mechanism the reference implementation uses, one layer down from Kubernetes Service
   DNS.

2. **A reverse-proxy block in the frontend's nginx image** puts the API on the same origin as the
   UI:

   ```nginx
   location /api/ {
       proxy_pass http://tictactoe-session:8082/;
       proxy_http_version 1.1;
       proxy_set_header Connection '';
       proxy_buffering off;        # required: nginx otherwise buffers SSE events
       proxy_read_timeout 1h;      # required: default 60s would sever the stream
   }
   ```

   with the dev-server equivalent in `vite.config.ts` so `npm run dev` behaves identically.

3. **`@CrossOrigin` and `VITE_SESSION_SERVICE_URL` are removed.** The frontend calls `/api/...`
   same-origin, so no CORS preflight occurs at all — a stricter outcome than centralising CORS
   configuration in a gateway.

4. **The SSE stream does not depend on the proxy staying connected.** Independently of this
   decision, and as recorded in the simulation flow requirements (OBS-5/OBS-6):
   `GET /sessions/{sessionId}` returns complete session state; failures are persisted *and* pushed
   as an explicit `failure` event rather than signalled by stream closure; and the frontend falls
   back to polling instead of closing on the first `onerror`. A severed stream therefore degrades
   to a slower UI, not a broken game.

5. **A periodic SSE heartbeat** (an SSE comment frame, invisible to `EventSource`) keeps the
   connection from appearing idle to any intermediary, and doubles as dead-peer detection for the
   emitter registry.

### Rejected: Eureka

No value at this scale. Two services, one instance each, static topology, no autoscaling. It
provides client-side load balancing across a single instance and costs a third JVM that must be
running before anything else works. The task authors run nine services without it.

### Rejected (for now): Spring Cloud Gateway

The one concern it would centralise — CORS — is eliminated more completely by same-origin serving.
Against that it adds a third JVM, a WebFlux module in an otherwise servlet-stack project, another
`depends_on` edge in the startup ordering, and the SSE timeout problem above.

**Had it been adopted, the SSE timeout would have been solved rather than inherited.** Spring Cloud
Gateway reads `response-timeout` and `connect-timeout` from per-route `metadata`, which overrides
the global value; a negative value disables the response timeout for that route alone. That is the
per-route scoping the reference implementation's TODO is asking for:

```yaml
routes:
  - id: session-events
    uri: http://tictactoe-session:8082
    predicates: [ Path=/api/sessions/*/events ]
    metadata:
      response-timeout: -1      # disabled for this route only
      connect-timeout: 2000     # connect still bounded
    order: 0                    # must precede the general /api/sessions/** route

  - id: session-api
    uri: http://tictactoe-session:8082
    predicates: [ Path=/api/sessions/** ]
    order: 1
```

This is recorded so the option is a configuration change rather than a rediscovery if the trigger
conditions below are met.

## Consequences

- One published port for the whole application; a reviewer runs `docker compose up` and opens a
  single URL. The service ports remain published for direct API inspection and Swagger UI.
- CORS disappears from application code. `SessionController` no longer encodes a deployment detail.
- Routing configuration lives in nginx and `vite.config.ts` — two places, both of which must be
  kept in step. A gateway would have made this one place, at the cost of a service.
- No rate limiting, no edge authentication, no gateway-level metrics. Rate limiting and metrics are
  out of scope per the root `../../../../Downloads/CLAUDE.md`; authorization is addressed in
  [0002](0002-security-model.md), deliberately in the session service rather than at the edge.
- The `proxy_buffering off` and `proxy_read_timeout` directives are load-bearing for SSE and easy
  to lose in a later nginx edit. They carry inline comments for that reason, and the SSE
  reconnect/rehydration path (item 4) is what keeps a mistake there from being a correctness bug.
- No multi-instance deployment story: a second `tictactoe-session` instance would not coordinate
  with the first beyond what the shared `session_db` gives it for free. Out of scope per the
  assignment.

## Revisit when

Any one of these makes the gateway worth its cost:

- A **third backend service**, at which point the frontend proxy becomes a routing table.
- **A second client** (mobile, CLI, another service), so edge concerns can no longer live in the
  frontend's web server.
- **Authentication** — as opposed to the session-scoped authorization of 0002 — which should
  terminate at the edge rather than being reimplemented per service.
- **Rate limiting or quota enforcement.**
- Deployment to **Kubernetes**, where an Ingress already occupies the edge-routing role and the
  marginal cost of Spring Cloud Gateway drops.

Service discovery specifically becomes relevant only when instance counts stop being fixed —
i.e. horizontal scaling or dynamic placement. Neither is in scope.
