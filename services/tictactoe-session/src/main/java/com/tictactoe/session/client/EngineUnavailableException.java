package com.tictactoe.session.client;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.ServiceUnavailableException;

/**
 * The engine is unreachable, timed out, or returned a server error. Retryable: a move is
 * idempotent per {@code (gameId, symbol, position)}, so retrying after this exception cannot
 * double-apply a move.
 */
public class EngineUnavailableException extends ServiceUnavailableException {

    public EngineUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
