package com.tictactoe.session.client;

public interface GameEngineClient {

    CreateGameResponse createGame(String gameId);

    ApplyMoveResponse move(String gameId, String player, int position);
}