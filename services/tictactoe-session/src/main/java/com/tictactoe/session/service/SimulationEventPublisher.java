package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.sse.SseEmitterRegistry;
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
        emitterRegistry.publish(sessionId, new SessionEvent(
                sessionId,
                new ArrayList<>(response.board()),
                response.stepStatus(),
                gameState != GameState.IN_PROGRESS ? gameState.toString() : null));
    }

    void complete(String sessionId) {
        emitterRegistry.complete(sessionId);
    }
}