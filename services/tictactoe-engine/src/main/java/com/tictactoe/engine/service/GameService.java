package com.tictactoe.engine.service;

import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.GameResponse;
import com.tictactoe.common.dto.MoveRequest;

public interface GameService {

    CreateGameResponse createGame(String gameId);

    GameResponse getGame(String gameId);

    MoveResponse applyMove(String gameId, MoveRequest move);
}