package com.tictactoe.common.error;

public class InternalException extends BaseException {

    public InternalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InternalException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}