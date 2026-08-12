# Testing

See the root `CLAUDE.md`'s "Testing Rules" section for the required coverage (all 8 win lines,
every rejection path, turn-order enforcement, mocked-engine scenarios from the session side, a
full create → simulate → poll integration flow, and concurrency tests) and determinism
requirements (no `Thread.sleep`, seeded `Random`, zero move delay in tests).

No tests exist yet — both services are still skeletons. This directory is reserved for testing
notes (e.g. how to run a specific slice, fixtures shared across test classes) as they come up.
