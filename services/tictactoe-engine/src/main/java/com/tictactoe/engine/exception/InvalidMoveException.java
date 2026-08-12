package com.tictactoe.engine.exception;

import org.springframework.http.HttpStatus;

public class InvalidMoveException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public InvalidMoveException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}