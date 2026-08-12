package com.tictactoe.session.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GameEngineClientImpl implements GameEngineClient {

    private final RestClient restClient;

    public GameEngineClientImpl(RestClient.Builder restClientBuilder,
                                 @Value("${game-engine.base-url}") String gameEngineBaseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(gameEngineBaseUrl)
                .build();
    }

    @Override
    public void createGame(String gameId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public GameEngineResponse move(String gameId, String player, int position) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}