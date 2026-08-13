package com.tictactoe.session.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class GameEngineClientImpl implements GameEngineClient {

    private final RestClient restClient;

    public GameEngineClientImpl(RestClient.Builder restClientBuilder,
                                 @Value("${game-engine.base-url}") String gameEngineBaseUrl,
                                 @Value("${game-engine.connect-timeout-ms:2000}") long connectTimeoutMs,
                                 @Value("${game-engine.read-timeout-ms:3000}") long readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        this.restClient = restClientBuilder
                .baseUrl(gameEngineBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public GameEngineResponse createGame(String gameId) {
        return restClient.post()
                .uri("/games")
                .body(new CreateGameRequestPayload(gameId))
                .retrieve()
                .body(GameEngineResponse.class);
    }

    @Override
    public GameEngineResponse move(String gameId, String player, int position) {
        return restClient.post()
                .uri("/games/{gameId}/move", gameId)
                .body(new MoveRequestPayload(player, position))
                .retrieve()
                .body(GameEngineResponse.class);
    }
}