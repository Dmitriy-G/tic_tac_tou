package com.tictactoe.engine.service;

import com.tictactoe.engine.dto.ApplyMoveResponse;
import com.tictactoe.engine.dto.CreateGameResponse;
import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.dto.MoveRequest;

public interface GameService {

    CreateGameResponse createGame(String gameId);

    GameResponse getGame(String gameId);

    ApplyMoveResponse applyMove(String gameId, MoveRequest move);
}