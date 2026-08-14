package com.tictactoe.session.domain;

import com.tictactoe.common.domain.StepStatus;

import java.util.List;

public record SessionEvent(String sessionId, EventType type, List<String> board, StepStatus stepStatus,
                            String winner, String errorCode, String errorMessage, String traceId) {

    public enum EventType {
        MOVE, COMPLETED, FAILED
    }
}
