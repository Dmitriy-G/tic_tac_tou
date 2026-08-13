package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;
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
    private final SessionStateStore stateStore;

    public SimulationEventPublisher(SseEmitterRegistry emitterRegistry, SessionStateStore stateStore) {
        this.emitterRegistry = emitterRegistry;
        this.stateStore = stateStore;
    }

    void publishMove(String sessionId, int moveNumber, String symbol, int position, MoveResponse response) {
        stateStore.recordMove(sessionId, moveNumber, Symbol.valueOf(symbol), position, response);

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

    /**
     * Errors after the 202 have no HTTP response left to fail into — this event is how the
     * client learns the simulation died, instead of inferring it from a stream that just stops.
     */
    void publishFailure(String sessionId, ErrorCode errorCode, String message) {
        stateStore.recordFailure(sessionId, errorCode, message);

        emitterRegistry.publish(sessionId, new SessionEvent(
                sessionId, SessionEvent.EventType.FAILED, null, null, null,
                errorCode.getCode(), message, MDC.get("traceId")));
    }

    void complete(String sessionId) {
        emitterRegistry.complete(sessionId);
    }
}
