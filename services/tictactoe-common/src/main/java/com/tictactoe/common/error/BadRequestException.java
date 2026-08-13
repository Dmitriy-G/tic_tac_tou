package com.tictactoe.common.error;

public class BadRequestException extends BaseException {

    public BadRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BadRequestException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}