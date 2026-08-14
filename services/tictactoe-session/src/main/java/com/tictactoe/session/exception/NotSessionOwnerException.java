package com.tictactoe.session.exception;

import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.ForbiddenException;

public class NotSessionOwnerException extends ForbiddenException {

    public NotSessionOwnerException(String sessionId) {
        super(ErrorCode.NOT_SESSION_OWNER, "Caller does not own session: " + sessionId);
    }
}