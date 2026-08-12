package com.tictactoe.engine.exception;

public class InvalidMoveException extends RuntimeException {

    public InvalidMoveException(String message) {
        super(message);
    }
}