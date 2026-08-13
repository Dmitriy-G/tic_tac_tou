package com.tictactoe.session.client;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.InternalException;

/**
 * The engine rejected a request with a 4xx other than 404 — the session built a bad request,
 * which is a bug in this service, not the caller's fault. Terminal: retrying a 400 just
 * produces the same 400 again.
 */
public class EngineContractViolationException extends InternalException {

    public EngineContractViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public EngineContractViolationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
