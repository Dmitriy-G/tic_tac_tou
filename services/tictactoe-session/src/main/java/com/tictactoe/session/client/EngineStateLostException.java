package com.tictactoe.session.client;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;

/**
 * The engine returned 404 for a game the session believes exists — the session's record is
 * stale, not the caller's fault. Terminal: never retried.
 */
public class EngineStateLostException extends BaseException {

    public EngineStateLostException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineStateLostException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
