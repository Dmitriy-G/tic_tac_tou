package com.tictactoe.gameengine.service;

import com.tictactoe.gameengine.model.Game;
import com.tictactoe.gameengine.model.MoveRequest;
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