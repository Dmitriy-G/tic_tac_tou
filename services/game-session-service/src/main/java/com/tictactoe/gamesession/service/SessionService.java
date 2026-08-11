package com.tictactoe.gamesession.service;

import com.tictactoe.gamesession.model.GameSession;

public interface SessionService {

    GameSession createSession();

    GameSession simulate(String sessionId);

    GameSession getSession(String sessionId);
}
