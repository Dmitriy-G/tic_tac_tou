package com.tictactoe.common.error;

public class ForbiddenException extends BaseException {

    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ForbiddenException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
