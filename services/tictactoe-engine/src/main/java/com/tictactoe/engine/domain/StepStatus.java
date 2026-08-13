package com.tictactoe.engine.domain;

public enum StepStatus {
    SUCCESS,
    GAME_FINISHED,
    INVALID_SYMBOL,
    INVALID_POSITION,
    CELL_OCCUPIED,
    OUT_OF_TURN
}