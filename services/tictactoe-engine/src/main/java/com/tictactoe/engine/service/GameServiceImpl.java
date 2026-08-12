package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.dto.MoveRequest;
import org.springframework.stereotype.Service;

@Service
public class GameServiceImpl implements GameService {

    @Override
    public Game createGame(String gameId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Game getGame(String gameId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Game applyMove(String gameId, MoveRequest move) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}