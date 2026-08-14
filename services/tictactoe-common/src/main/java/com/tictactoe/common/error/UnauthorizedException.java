package com.tictactoe.common.error;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public UnauthorizedException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
