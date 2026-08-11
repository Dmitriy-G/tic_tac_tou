package com.tictactoe.gameengine.service;

import com.tictactoe.gameengine.model.Game;
import com.tictactoe.gameengine.model.MoveRequest;

public interface GameService {

    Game createGame(String gameId);

    Game getGame(String gameId);

    Game applyMove(String gameId, MoveRequest move);
}