package com.tictactoe.session.exception;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.NotFoundException;

public class SessionNotFoundException extends NotFoundException {

    public SessionNotFoundException(String sessionId) {
        super(ErrorCode.SESSION_NOT_FOUND, "Session not found: " + sessionId);
    }
}
