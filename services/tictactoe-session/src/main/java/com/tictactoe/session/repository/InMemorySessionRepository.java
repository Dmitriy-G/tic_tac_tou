package com.tictactoe.session.repository;

import com.tictactoe.session.domain.GameSession;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    @Override
    public GameSession save(GameSession session) {
        sessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public Optional<GameSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}