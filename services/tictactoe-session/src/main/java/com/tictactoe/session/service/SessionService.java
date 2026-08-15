package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.exception.NotSessionOwnerException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import com.tictactoe.session.util.SessionIdUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class SessionService {

    private final SessionJpaRepository sessionRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final SimulationStarter simulationStarter;
    private final SessionStateStore stateStore;
    private final OwnerTokenService ownerTokenService;

    public SessionService(SessionJpaRepository sessionRepository,
                          SseEmitterRegistry emitterRegistry,
                          SimulationStarter simulationStarter,
                          SessionStateStore stateStore,
                          OwnerTokenService ownerTokenService) {
        this.sessionRepository = sessionRepository;
        this.emitterRegistry = emitterRegistry;
        this.simulationStarter = simulationStarter;
        this.stateStore = stateStore;
        this.ownerTokenService = ownerTokenService;
    }

    public SessionResponse createSession() {
        UUID sessionId = UUID.randomUUID();
        String ownerToken = ownerTokenService.generate();

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash(ownerTokenService.hash(ownerToken));
        sessionRepository.save(session);
        stateStore.initialize(sessionId.toString());

        return toResponse(sessionId.toString(), ownerToken);
    }

    public void simulate(String sessionId, String ownerToken) {
        SessionEntity session = sessionRepository.findById(SessionIdUtils.toUuid(sessionId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (!ownerTokenService.matches(ownerToken, session.getOwnerTokenHash())) {
            throw new NotSessionOwnerException(sessionId);
        }
        simulationStarter.start(sessionId);
    }

    public SessionResponse getSession(String sessionId) {
        if (!sessionRepository.existsById(SessionIdUtils.toUuid(sessionId))) {
            throw new SessionNotFoundException(sessionId);
        }
        return toResponse(sessionId, null);
    }

    public SseEmitter subscribe(String sessionId, String lastEventId) {
        getSession(sessionId);
        return emitterRegistry.register(sessionId, lastEventId);
    }

    private SessionResponse toResponse(String sessionId, String ownerToken) {
        SessionStateStore.LiveState liveState = stateStore.get(sessionId);
        return new SessionResponse(sessionId, liveState.status(), liveState.board(), liveState.moves(),
                liveState.winner(), liveState.errorCode(), liveState.errorMessage(), ownerToken);
    }
}