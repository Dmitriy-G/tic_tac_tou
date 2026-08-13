package com.tictactoe.common.error;

public class ServiceUnavailableException extends BaseException {

    public ServiceUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ServiceUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}