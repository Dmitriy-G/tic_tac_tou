package com.tictactoe.session.client;

public interface GameEngineClient {

    GameEngineResponse createGame(String gameId);

    GameEngineResponse move(String gameId, String player, int position);
}