package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class SimulationEventPublisher {

    private final SseEmitterRegistry emitterRegistry;

    public SimulationEventPublisher(SseEmitterRegistry emitterRegistry) {
        this.emitterRegistry = emitterRegistry;
    }

    void publishMove(String sessionId, MoveResponse response) {
        GameState gameState = response.gameState();
        boolean completed = gameState != GameState.IN_PROGRESS;
        emitterRegistry.publish(sessionId, new SessionEvent(
                sessionId,
                completed ? SessionEvent.EventType.COMPLETED : SessionEvent.EventType.MOVE,
                new ArrayList<>(response.board()),
                response.stepStatus(),
                completed ? gameState.toString() : null,
                null,
                null,
                MDC.get("traceId")));
    }

    void publishFailure(String sessionId, ErrorCode errorCode, String message) {
        emitterRegistry.publish(sessionId, new SessionEvent(
                sessionId, SessionEvent.EventType.FAILED, null, null, null,
                errorCode.getCode(), message, MDC.get("traceId")));
    }

    void complete(String sessionId) {
        emitterRegistry.complete(sessionId);
    }
}