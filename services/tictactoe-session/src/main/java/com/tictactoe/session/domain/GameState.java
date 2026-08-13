package com.tictactoe.session.domain;

//TODO: Move to library
public enum GameState {
    CREATED,
    IN_PROGRESS,
    X_WON,
    O_WON,
    DRAW,
    FAILED,
    CANCELLED
}