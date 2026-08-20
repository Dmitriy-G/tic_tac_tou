package com.tictactoe.session.exception;

import com.tictactoe.common.error.ConflictException;
import com.tictactoe.common.error.ErrorCode;

public class SessionAlreadyCompletedException extends ConflictException {

    public SessionAlreadyCompletedException(String sessionId) {
        super(ErrorCode.SESSION_ALREADY_COMPLETED, "Session already completed and cannot be re-simulated: " + sessionId);
    }
}
