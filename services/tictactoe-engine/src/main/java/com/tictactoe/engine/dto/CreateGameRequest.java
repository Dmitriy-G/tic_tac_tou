package com.tictactoe.engine.dto;

/**
 * gameId is optional: the session service supplies it so that gameId == sessionId (see root
 * CLAUDE.md - "sessionId IS the gameId"); if omitted, the engine generates one itself.
 */
public record CreateGameRequest(String gameId) {
}