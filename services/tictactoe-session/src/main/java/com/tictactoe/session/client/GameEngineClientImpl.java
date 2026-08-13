package com.tictactoe.session.client;

import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.dto.CreateGameRequest;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.MoveRequest;
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
    public CreateGameResponse createGame(String gameId) {
        return restClient.post()
                .uri("/games")
                .body(new CreateGameRequest(gameId))
                .retrieve()
                .body(CreateGameResponse.class);
    }

    @Override
    public MoveResponse move(String gameId, String player, int position) {
        return restClient.post()
                .uri("/games/{gameId}/move", gameId)
                .body(new MoveRequest(Symbol.valueOf(player), position))
                .retrieve()
                .body(MoveResponse.class);
    }
}