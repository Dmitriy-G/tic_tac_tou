package com.tictactoe.session.client;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;

public class EngineStateLostException extends BaseException {

    public EngineStateLostException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineStateLostException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
