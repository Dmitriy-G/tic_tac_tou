package com.tictactoe.session.client;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;

/**
 * The engine's response couldn't be parsed or was otherwise unusable (missing body, unexpected
 * status). The contract between the two services is broken. Terminal: never retried.
 */
public class EngineBadResponseException extends BaseException {

    public EngineBadResponseException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineBadResponseException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
