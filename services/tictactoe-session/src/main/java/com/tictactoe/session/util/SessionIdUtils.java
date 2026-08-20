package com.tictactoe.session.util;

import com.tictactoe.common.error.BadRequestException;
import com.tictactoe.common.error.ErrorCode;

import java.util.UUID;

public final class SessionIdUtils {

    private SessionIdUtils() {
    }

    public static UUID toUuid(String sessionId) {
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_SESSION_ID, "Malformed session id: " + sessionId, e);
        }
    }
}
