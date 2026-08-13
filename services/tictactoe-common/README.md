# tictactoe-common

Shared library for the engine and session services: the `GameState`, `Symbol`, and `StepStatus`
enums, and the wire DTOs (`CreateGameRequest`, `CreateGameResponse`, `MoveRequest`,
`MoveResponse`, `GameResponse`) exchanged over the engine's `/games` HTTP API.

A plain JAR with no Spring or persistence dependencies — no business logic, no entities, no
controllers. It is not a runnable service and has no Docker image.
