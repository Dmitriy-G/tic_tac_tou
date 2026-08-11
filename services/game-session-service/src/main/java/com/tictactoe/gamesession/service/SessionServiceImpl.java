package com.tictactoe.gamesession.service;

import com.tictactoe.gamesession.client.GameEngineClient;
import com.tictactoe.gamesession.model.GameSession;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl implements SessionService {

    private final GameEngineClient gameEngineClient;

    public SessionServiceImpl(GameEngineClient gameEngineClient) {
        this.gameEngineClient = gameEngineClient;
    }

    @Override
    public GameSession createSession() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public GameSession simulate(String sessionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public GameSession getSession(String sessionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
