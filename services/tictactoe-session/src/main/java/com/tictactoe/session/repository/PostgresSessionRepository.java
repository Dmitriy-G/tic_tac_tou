package com.tictactoe.session.repository;

import com.tictactoe.session.domain.GameSession;
import com.tictactoe.session.domain.SessionStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code sessions} table only durably tracks session identity (see V1 migration) - it never
 * stores the board (engine's job) or move history (would duplicate history the engine already
 * owns). The live, in-flight {status, moveHistory} of a session is process-local state; it is
 * kept here in memory only, alongside the persisted identity row.
 */
@Repository
public class PostgresSessionRepository implements SessionRepository {

    private final SessionJpaRepository sessionJpaRepository;
    private final SimulationRepository simulationRepository;
    private final Map<String, GameSession> liveSessions = new ConcurrentHashMap<>();

    public PostgresSessionRepository(SessionJpaRepository sessionJpaRepository,
                                      SimulationRepository simulationRepository) {
        this.sessionJpaRepository = sessionJpaRepository;
        this.simulationRepository = simulationRepository;
    }

    @Override
    public GameSession save(GameSession session) {
        SessionEntity entity = new SessionEntity();
        entity.setId(UUID.fromString(session.getSessionId()));
        sessionJpaRepository.save(entity);
        liveSessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public Optional<GameSession> findById(String sessionId) {
        GameSession cached = liveSessions.get(sessionId);
        if (cached != null) {
            return Optional.of(cached);
        }

        return sessionJpaRepository.findById(UUID.fromString(sessionId))
                .map(entity -> reconstruct(entity.getId().toString()));
    }

    private GameSession reconstruct(String sessionId) {
        GameSession session = new GameSession();
        session.setSessionId(sessionId);
        session.setGameId(sessionId);
        session.setMoveHistory(new ArrayList<>());
        session.setStatus(simulationRepository.findLatestBySessionId(sessionId)
                .map(simulation -> simulation.getStatus())
                .orElse(SessionStatus.CREATED));
        return session;
    }
}