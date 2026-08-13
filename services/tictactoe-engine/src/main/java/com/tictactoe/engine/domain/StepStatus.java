package com.tictactoe.engine.domain;

public enum StepStatus {
    CORRECT_STEP,
    GAME_FINISHED,
    INVALID_SYMBOL,
    INVALID_POSITION,
    CELL_OCCUPIED,
    OUT_OF_TURN
}