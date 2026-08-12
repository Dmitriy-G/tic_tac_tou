package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.dto.MoveRequest;

public interface GameService {

    Game createGame(String gameId);

    Game getGame(String gameId);

    Game applyMove(String gameId, MoveRequest move);
}