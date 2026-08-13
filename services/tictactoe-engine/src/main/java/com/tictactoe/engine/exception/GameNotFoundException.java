package com.tictactoe.engine.exception;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.NotFoundException;

public class GameNotFoundException extends NotFoundException {

    public GameNotFoundException(String gameId) {
        super(ErrorCode.GAME_NOT_FOUND, "Game not found: " + gameId);
    }
}
