package com.tictactoe.engine.domain;

public enum GameState {
    CREATED,
    IN_PROGRESS,
    X_WON,
    O_WON,
    DRAW,
    FAILED,
    CANCELLED
}