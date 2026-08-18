# 0004: Session is the game — collapse `simulations` into `sessions`

## Status

Accepted.

Related: builds on the compare-and-swap idiom introduced in
[0003](0003-optimistic-concurrency-on-moves.md) — the double-simulate guard here uses the same
mechanism, so the codebase now has one concurrency idiom instead of two.

## Context

`session_db` modelled `sessions 1 —— N simulations`: a foreign key from `simulations.session_id`,
a `List`-returning finder (`findBySessionIdOrderByStartedAtDesc`), and a unique partial index
(`uq_simulation_running`) to stop two simulations running for the same session at once. But
`SimulationStarter` capped a session at exactly one simulation ever — a second `/simulate` call on
a session with any prior run, running or finished, was always rejected. The schema encoded a
one-to-many relationship the code never actually allowed.

`task.pdf`'s Game Session Service section treats session and game as the same thing:

> Generate a unique sessionId (which may also serve as the gameId for the Game Engine Service).
> Retrieve session details, including **the** current game state and move history.

Singular game state, singular history, sessionId offered as the gameId. Three existing problems
were symptoms of the same mismatch:

- **Three identities for one concept.** `sessionId`, `simulationId`, and the engine's `gameId`
  were three UUIDs where the specification only ever describes one.
- **A false claim in the root `CLAUDE.md`.** Its Storage section already asserted *"`sessionId`
  **is** `gameId` so no separate column is needed"* — true of the column, but the session service
  still generated and threaded a distinct `simulationId` everywhere internally.
- **`GET /sessions/{id}` didn't survive a restart.** It read exclusively from the in-memory
  `SessionStateStore`; a fresh JVM reported `CREATED` with an empty board for a session that had
  actually finished, because nothing about its outcome lived in the database.

## Decision

Collapse `simulations` into `sessions`. One session is one game:

- `sessions` gains `status`, `board`, `winner`, `errors_count`, `error_code`, `error_message`,
  `started_at`, `finished_at` (`V5__collapse_simulations_into_sessions.sql`) and becomes the
  single source of truth for a game's state — not just an identity/anchor row.
- A new `session_moves` table (`session_id`, `move_number`, `symbol`, `position`, `step_status`,
  `created_at`, composite primary key `(session_id, move_number)`) makes the move history durable
  — "session and move data" per the spec — replacing the in-memory move list. Only moves that
  actually land (`CORRECT_STEP`) get a row: a rejected attempt doesn't advance `move_number`, so
  persisting every attempt would collide with the composite key by design, not by accident.
- `simulations` and `running_session_id` are dropped entirely. The double-simulate guard becomes
  a single conditional `UPDATE`:

  ```sql
  UPDATE sessions SET status = 'IN_PROGRESS', started_at = :startedAt
   WHERE id = :id AND status = 'CREATED'
  ```

  Row count `1` means this caller claimed the session; `0` means someone else already did, or it
  was never `CREATED`. This is strictly stronger than the old three-read-then-insert-then-
  catch-the-unique-violation: there is no window between check and claim for two concurrent
  callers to both pass, and it's the same CAS idiom [0003](0003-optimistic-concurrency-on-moves.md)
  established for the engine's board.
- `gameId == sessionId` for real: `SimulationRunner`/`SimulationStep` call
  `gameEngineClient.createGame(sessionId)`/`move(sessionId, ...)` directly. The separate
  `simulationId` is gone from every signature.
- `SessionStateStore` (the in-memory `LiveState` map) is deleted. `SessionService.getSession`
  rebuilds the full view — status, board, moves, winner, error — from `sessions` and
  `session_moves` alone, which is what makes it survive a process restart.
- Persistence happens per move: one `INSERT` into `session_moves` plus one `UPDATE` on `sessions`
  per accepted move (`SessionStateWriter.recordMove`), settling the previously-undocumented
  "persist on each step vs. after simulation" question in favour of per-step. See Consequences.

## Rejected

**Session-as-browser-session, with N games per session.** A defensible domain model in the
abstract — a "session" could outlive any one game, letting a client start several games under one
identity — and the discarded schema (a real `simulations` foreign key, a list-returning finder)
already leaned this way. Rejected because it contradicts the specification's singular *"the
current game state"* and *"the current game state and move history"* (not *"the games"* or *"a
history of games"*), and it buys nothing for a UI that only ever runs one game at a time and mints
a fresh session per click regardless (`useSimulation.start`). Revisit if a "run again, keep
history" button or multiple concurrent games per client ever gets built — either would need
sessions and games to be separate concepts again, with the 1:N schema this ADR removes.

**Keeping the `simulations` table alongside the new `sessions` columns**, treating one as a cache
of the other. Two tables that must always agree on the same nine facts is the exact
duplicate-source-of-truth shape `GET /sessions/{id}`'s restart bug came from in the first place;
collapsing to one table removes the class of bug rather than papering over one instance of it.

## Consequences

- `GET /sessions/{id}` returns the correct terminal state for a session whose simulation ran (and
  finished, or crashed) in a process that has since restarted — the concrete case
  `getSessionAfterARestartStillReportsTheTerminalState` in `SessionServiceTest` exists to prove.
- The double-simulate guard requires no unique index and no `DataIntegrityViolationException`
  translation — a plain `int` return tells the caller everything needed.
- Per-move persistence (one `INSERT` + one `UPDATE` per move) is fine for a nine-move game and is
  what makes state durable, but is a real scaling ceiling: it would not be the right cadence for
  batch/mass simulation of many concurrent games, which is explicitly out of scope for this
  project (root `CLAUDE.md`'s Out of Scope list) but worth naming here since it's the first place
  the choice would bite.
- `errors_count`/`error_code`/`error_message` now live on `sessions` directly; a session that
  fails after some successful moves still shows those moves in `session_moves`, but the rejected
  attempts that pushed it over the error budget are not individually recorded — only their count
  is. This mirrors what the UI's move log ever showed (a numbered log of the game itself, not of
  every retried attempt).
- One fewer identifier throughout the codebase and its logs: `sessionId` is the only UUID a
  request, a log line, or a trace ever needs to carry for a given game.

## Revisit when

- **A "run it again" feature** wants to keep a session's history across multiple games — this ADR
  would need to be superseded, not amended, since it removes the schema shape that would support
  it.
- **Mass/batch simulation** becomes in scope, at which point the per-move write cadence in
  Consequences needs a different persistence strategy (e.g. batching writes, or persisting only at
  terminal state with an in-memory buffer for the live view).