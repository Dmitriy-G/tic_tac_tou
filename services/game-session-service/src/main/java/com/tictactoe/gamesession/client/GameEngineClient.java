package com.tictactoe.gamesession.client;

public interface GameEngineClient {

    void createGame(String gameId);

    GameEngineResponse move(String gameId, String player, int position);
}
