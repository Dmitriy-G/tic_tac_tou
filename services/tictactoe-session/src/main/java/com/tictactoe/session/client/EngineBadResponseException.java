package com.tictactoe.session.client;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;

public class EngineBadResponseException extends BaseException {

    public EngineBadResponseException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineBadResponseException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
