package com.tictactoe.session.client;

import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.dto.CreateGameResponse;

public interface GameEngineClient {

    CreateGameResponse createGame(String gameId);

    MoveResponse move(String gameId, String player, int position);
}