package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.dto.MoveRecord;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.exception.NotSessionOwnerException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SessionMoveJpaRepository;
import com.tictactoe.session.repository.SessionMoveMapper;
import com.tictactoe.session.sse.SseEmitterRegistry;
import com.tictactoe.session.util.BoardUtils;
import com.tictactoe.session.util.SessionIdUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionJpaRepository sessionRepository;
    private final SessionMoveJpaRepository moveRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final SimulationStarter simulationStarter;
    private final OwnerTokenService ownerTokenService;

    public SessionService(SessionJpaRepository sessionRepository,
                          SessionMoveJpaRepository moveRepository,
                          SseEmitterRegistry emitterRegistry,
                          SimulationStarter simulationStarter,
                          OwnerTokenService ownerTokenService) {
        this.sessionRepository = sessionRepository;
        this.moveRepository = moveRepository;
        this.emitterRegistry = emitterRegistry;
        this.simulationStarter = simulationStarter;
        this.ownerTokenService = ownerTokenService;
    }

    public SessionResponse createSession() {
        UUID sessionId = UUID.randomUUID();
        String ownerToken = ownerTokenService.generate();

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash(ownerTokenService.hash(ownerToken));
        session.setStatus(GameState.CREATED);
        session.setBoard(BoardUtils.convertToString(BoardUtils.emptyBoard()));
        session.setErrorsCount(0);
        sessionRepository.save(session);

        return new SessionResponse(sessionId.toString(), GameState.CREATED, BoardUtils.emptyBoard(),
                List.of(), null, null, null, ownerToken);
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
        return toResponse(sessionId, null);
    }

    public SseEmitter subscribe(String sessionId, String lastEventId) {
        if (!sessionRepository.existsById(SessionIdUtils.toUuid(sessionId))) {
            throw new SessionNotFoundException(sessionId);
        }
        return emitterRegistry.register(sessionId, lastEventId);
    }

    private SessionResponse toResponse(String sessionId, String ownerToken) {
        SessionEntity session = sessionRepository.findById(SessionIdUtils.toUuid(sessionId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        List<MoveRecord> moves = moveRepository.findBySessionIdOrderByMoveNumber(session.getId())
                .stream().map(SessionMoveMapper::toRecord).toList();
        return new SessionResponse(sessionId, session.getStatus(), BoardUtils.convertToList(session.getBoard()),
                moves, session.getWinner(), session.getErrorCode(), session.getErrorMessage(), ownerToken);
    }
}