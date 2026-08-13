package com.tictactoe.session.domain;

import com.tictactoe.common.domain.StepStatus;

import java.util.List;

/**
 * SSE payload. {@code type} discriminates what the client should do with the event: {@code MOVE}
 * is an in-progress board update, {@code COMPLETED} is a terminal engine outcome (win/draw),
 * {@code FAILED} is a simulation fault with no further board updates coming. A closed stream
 * alone is indistinguishable from a network blip, so faults must always arrive as an explicit
 * {@code FAILED} event before the stream completes.
 */
public record SessionEvent(String sessionId, EventType type, List<String> board, StepStatus stepStatus,
                            String winner, String errorCode, String errorMessage, String traceId) {

    public enum EventType {
        MOVE, COMPLETED, FAILED
    }
}
