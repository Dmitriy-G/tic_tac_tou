package com.tictactoe.session.client;

import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.CreateGameRequest;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.config.CorrelationIdFilter;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class GameEngineClientImpl implements GameEngineClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final RestClient restClient;
    private final DefaultResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
    private final Retry retry;
    private final String internalToken;

    @Autowired
    public GameEngineClientImpl(RestClient.Builder restClientBuilder,
                                 @Value("${game-engine.base-url}") String gameEngineBaseUrl,
                                 @Value("${game-engine.connect-timeout-ms:2000}") long connectTimeoutMs,
                                 @Value("${game-engine.read-timeout-ms:3000}") long readTimeoutMs,
                                 @Value("${game-engine.retry.max-attempts:3}") int retryMaxAttempts,
                                 @Value("${game-engine.retry.initial-backoff-ms:200}") long retryInitialBackoffMs,
                                 @Value("${internal.token}") String internalToken) {
        this(buildRestClient(restClientBuilder, gameEngineBaseUrl, connectTimeoutMs, readTimeoutMs),
                retryMaxAttempts, retryInitialBackoffMs, internalToken);
    }

    GameEngineClientImpl(RestClient restClient, int retryMaxAttempts, long retryInitialBackoffMs, String internalToken) {
        this.restClient = restClient;
        this.internalToken = internalToken;
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryMaxAttempts)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(retryInitialBackoffMs)))
                .retryOnException(e -> e instanceof EngineUnavailableException)
                .build();
        this.retry = Retry.of("gameEngineClient", retryConfig);
    }

    private static RestClient buildRestClient(RestClient.Builder restClientBuilder, String gameEngineBaseUrl,
                                               long connectTimeoutMs, long readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return restClientBuilder
                .baseUrl(gameEngineBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public CreateGameResponse createGame(String gameId) {
        return withRetry(() -> doCreateGame(gameId));
    }

    @Override
    public MoveResponse move(String gameId, String player, int position) {
        return withRetry(() -> doMove(gameId, player, position));
    }

    private CreateGameResponse doCreateGame(String gameId) {
        try {
            CreateGameResponse response = restClient.post()
                    .uri("/games")
                    .header(CorrelationIdFilter.HEADER, correlationId())
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(new CreateGameRequest(gameId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> errorHandler.handleError(res))
                    .body(CreateGameResponse.class);
            if (response == null || response.board() == null) {
                throw new EngineBadResponseException(ErrorCode.ENGINE_BAD_RESPONSE,
                        "Engine returned an incomplete response for game creation: " + gameId);
            }
            return response;
        } catch (EngineBadResponseException e) {
            throw e;
        } catch (Exception e) {
            throw classify(e, gameId);
        }
    }

    private MoveResponse doMove(String gameId, String player, int position) {
        try {
            return restClient.post()
                    .uri("/games/{gameId}/move", gameId)
                    .header(CorrelationIdFilter.HEADER, correlationId())
                    .header(INTERNAL_TOKEN_HEADER, internalToken)
                    .body(new MoveRequest(Symbol.valueOf(player), position))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> errorHandler.handleError(res))
                    .body(MoveResponse.class);
        } catch (Exception e) {
            throw classify(e, gameId);
        }
    }

    private <T> T withRetry(Supplier<T> call) {
        return Retry.decorateSupplier(retry, call).get();
    }

    private static String correlationId() {
        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return traceId != null ? traceId : "";
    }

    private RuntimeException classify(Exception e, String gameId) {
        if (e instanceof ResourceAccessException) {
            return new EngineUnavailableException(ErrorCode.ENGINE_UNAVAILABLE,
                    "Engine unreachable for game " + gameId, e);
        }
        if (e instanceof HttpServerErrorException) {
            return new EngineUnavailableException(ErrorCode.ENGINE_UNAVAILABLE,
                    "Engine returned a server error for game " + gameId, e);
        }
        if (e instanceof HttpClientErrorException.NotFound) {
            return new EngineStateLostException(ErrorCode.ENGINE_STATE_LOST,
                    "Engine has no record of game " + gameId, e);
        }
        if (e instanceof HttpClientErrorException) {
            return new EngineContractViolationException(ErrorCode.ENGINE_CONTRACT_VIOLATION,
                    "Session service sent an invalid request to the engine for game " + gameId, e);
        }
        return new EngineBadResponseException(ErrorCode.ENGINE_BAD_RESPONSE,
                "Unexpected or unparseable response from engine for game " + gameId, e);
    }
}
