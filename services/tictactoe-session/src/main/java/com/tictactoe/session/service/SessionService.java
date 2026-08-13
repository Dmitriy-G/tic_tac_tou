package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class SessionService {

    private final SessionJpaRepository sessionRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final SimulationStarter simulationStarter;
    private final SessionStateStore stateStore;

    public SessionService(SessionJpaRepository sessionRepository,
                          SseEmitterRegistry emitterRegistry,
                          SimulationStarter simulationStarter,
                          SessionStateStore stateStore) {
        this.sessionRepository = sessionRepository;
        this.emitterRegistry = emitterRegistry;
        this.simulationStarter = simulationStarter;
        this.stateStore = stateStore;
    }

    public SessionResponse createSession() {
        UUID sessionId = UUID.randomUUID();

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        sessionRepository.save(session);
        stateStore.initialize(sessionId.toString());

        return toResponse(sessionId.toString());
    }

    public void simulate(String sessionId) {
        simulationStarter.start(sessionId);
    }

    /**
     * Rebuilds the full session view from {@link SessionStateStore} — status, board, move
     * history, winner and any failure — so SSE is an optimisation on top of this, not the only
     * way to observe a session. A page refresh or a lost stream can always fall back here.
     */
    public SessionResponse getSession(String sessionId) {
        if (!sessionRepository.existsById(UUID.fromString(sessionId))) {
            throw new SessionNotFoundException(sessionId);
        }
        return toResponse(sessionId);
    }

    public SseEmitter subscribe(String sessionId, String lastEventId) {
        getSession(sessionId); // existence check only; result intentionally discarded
        return emitterRegistry.register(sessionId, lastEventId);
    }

    private SessionResponse toResponse(String sessionId) {
        SessionStateStore.LiveState liveState = stateStore.get(sessionId);
        return new SessionResponse(sessionId, liveState.status(), liveState.board(), liveState.moves(),
                liveState.winner(), liveState.errorCode(), liveState.errorMessage());
    }
}
