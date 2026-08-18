# 0003: Optimistic concurrency (compare-and-swap) on move persistence

## Status

Accepted.

## Context

`GameService.applyMove` was `load → validate → save`, with nothing between the read and the write:

```java
Game game = gameStore.load(gameId);
StepStatus status = moveValidator.validate(board, game.state(), move);
board.set(move.position(), move.symbol().name());
gameStore.save(game, board, outcome.state());
```

`GameStore.save` wrote the whole 9-character board unconditionally. Two overlapping calls to
`applyMove` on the same game therefore raced as a classic lost update: both threads read the same
starting board, both validated against it, and the second `save` silently clobbered the first
regardless of which cells either move touched.

`GameStoreConcurrencyTest` proved this against the real beans and the H2 test database: eight
threads calling `applyMove(gameId, X, 4)` on a fresh game produced eight `CORRECT_STEP` results
instead of one. `docs/adr/0002-security-model.md` and the engine README both asserted
concurrent-move safety that, until this change, did not exist.

Tic-tac-toe has strict turn order — at any instant exactly one symbol may legally move — so the
domain property to defend is narrower than "nine concurrent writes to nine cells should all land."
That would describe a system that has stopped enforcing its own rules. The property that actually
holds is: **concurrent attempts on the same game leave exactly one accepted move**, and every
loser gets the honest rejection reason (`CELL_OCCUPIED` or `OUT_OF_TURN`), not a synthetic error.

## Decision

Compare-and-swap on the `games.board` column, plus a bounded re-validation loop in
`GameService.applyMove`.

`GameJpaRepository.compareAndSwapBoard` is a `@Modifying` query that writes the new board and state
only if the stored board still equals the board the caller read:

```sql
UPDATE games SET board = :newBoard, state = :state
 WHERE id = :id AND board = :expectedBoard
```

It returns the row count: `1` means this call won the race, `0` means another writer committed
first. `GameStore.compareAndSave` wraps this and returns a `boolean`; the old unconditional `save`
is gone.

`GameService.applyMove` loops up to `MAX_ATTEMPTS = 3` times. Each iteration re-loads the game,
re-validates the move against whatever is currently stored, and — if still valid — attempts the
compare-and-swap. The loop is not "retry the write until it succeeds": a losing thread's *next*
iteration almost always exits through the rejection branch, because the board it re-reads now
carries the winning move. The loop exists so the caller receives the true `StepStatus` rather than
a made-up conflict. Exhausting all attempts (effectively impossible for a single session driving a
nine-move game) throws `ConflictException` with `ErrorCode.CONFLICT` (409) — a well-formed,
retryable request, not `INTERNAL_ERROR`.

`@Transactional` is applied only to the `@Modifying` query in `GameJpaRepository`, per Spring
Data's requirement for modifying queries — not as application-level locking. The row lock it
implies lasts only for that single `UPDATE` statement's duration. `GameService.applyMove` itself
carries no `@Transactional`: a transaction spanning the whole load-validate-save cycle would pin
the read's snapshot for its duration and defeat the point of the CAS loop, which depends on each
iteration seeing the latest committed board.

## Rejected

- **In-process `synchronized` or a per-game `ReentrantLock`.** Works only within a single JVM.
  Silently stops providing any safety the moment a second engine instance runs — precisely the
  deployment the microservice split implies. Excluded on principle, not just because it's the
  wrong tool here.
- **`@Version` optimistic locking.** Would work, but costs a schema column and migration the board
  string doesn't otherwise need, and turns a lost race into an
  `ObjectOptimisticLockingFailureException` that has to be caught and translated back into domain
  terms. An `int` row count needs no translation.
- **`SELECT ... FOR UPDATE`.** A pessimistic row lock held across validation and outcome
  evaluation, not just the write. Heavier than necessary, and it makes lock ordering something a
  reader of the code has to reason about even though the actual contention window (one `UPDATE`)
  is tiny.

## Consequences

- Correctness no longer depends on running exactly one engine instance — the CAS predicate is
  enforced by the database, not by anything in process memory.
- A losing caller pays one extra read-validate cycle per lost race; with `MAX_ATTEMPTS = 3` this is
  bounded and, for the actual traffic pattern (one session driving both players sequentially),
  essentially never observed.
- `MAX_ATTEMPTS` exhaustion surfaces as a `409 CONFLICT` rather than a wrong answer or a hang.
- `GameStore.save` no longer exists; its only caller, `GameService.applyMove`, now goes through
  `compareAndSave` and handles the `false` case explicitly.
- No schema change: the CAS predicate is the existing `board` column, not a new `version` column.