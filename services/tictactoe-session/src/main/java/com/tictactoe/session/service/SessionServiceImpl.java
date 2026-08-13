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
public class SessionServiceImpl implements SessionService {

    private final SessionJpaRepository sessionRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final SimulationStarter simulationStarter;

    public SessionServiceImpl(SessionJpaRepository sessionRepository,
                               SseEmitterRegistry emitterRegistry,
                               SimulationStarter simulationStarter) {
        this.sessionRepository = sessionRepository;
        this.emitterRegistry = emitterRegistry;
        this.simulationStarter = simulationStarter;
    }

    @Override
    public SessionResponse createSession() {
        UUID sessionId = UUID.randomUUID();

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        sessionRepository.save(session);

        return new SessionResponse(sessionId.toString());
    }

    @Override
    public void simulate(String sessionId) {
        simulationStarter.start(sessionId);
    }

    @Override
    public SessionResponse getSession(String sessionId) {
        SessionEntity session = sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        return new SessionResponse(session.getId().toString());
    }

    @Override
    public SseEmitter subscribe(String sessionId) {
        getSession(sessionId); // existence check only; result intentionally discarded
        return emitterRegistry.register(sessionId);
    }
}