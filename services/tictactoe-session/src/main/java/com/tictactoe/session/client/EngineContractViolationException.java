package com.tictactoe.session.client;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.InternalException;

public class EngineContractViolationException extends InternalException {

    public EngineContractViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineContractViolationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
