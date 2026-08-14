package com.tictactoe.session.client;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.ServiceUnavailableException;

public class EngineUnavailableException extends ServiceUnavailableException {

    public EngineUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
