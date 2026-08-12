package com.tictactoe.session.repository;

import com.tictactoe.session.domain.GameSession;

import java.util.Optional;

public interface SessionRepository {

    GameSession save(GameSession session);

    Optional<GameSession> findById(String sessionId);
}