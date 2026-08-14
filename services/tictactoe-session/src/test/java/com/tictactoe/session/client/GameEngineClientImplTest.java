package com.tictactoe.session.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * One test per {@link GameEngineClientImpl#classify} branch, plus retry firing only for
 * {@link EngineUnavailableException}. {@code retryMaxAttempts=3, retryInitialBackoffMs=1} keeps
 * the retrying tests fast and deterministic — no {@code Thread.sleep} in this test either way,
 * the backoff is the retry library's own, driven down to ~1ms.
 */
class GameEngineClientImplTest {

    private GameEngineClientImpl clientWithRetries(int maxAttempts) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.baseUrl("http://engine").build();
        return new GameEngineClientImpl(restClient, maxAttempts, 1, "test-internal-token");
    }

    private MockRestServiceServer server;

    @Test
    void timeoutIsClassifiedAsEngineUnavailableAndRetried() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("timed out");
                });
        server.expect(requestTo("http://engine/games"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("timed out");
                });
        server.expect(requestTo("http://engine/games"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("timed out");
                });

        assertThatThrownBy(() -> client.createGame("game-1"))
                .isInstanceOf(EngineUnavailableException.class);

        server.verify();
    }

    @Test
    void serverErrorIsClassifiedAsEngineUnavailableAndRetried() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games")).andRespond(withServerError());
        server.expect(requestTo("http://engine/games")).andRespond(withServerError());
        server.expect(requestTo("http://engine/games")).andRespond(withServerError());

        assertThatThrownBy(() -> client.createGame("game-1"))
                .isInstanceOf(EngineUnavailableException.class);

        server.verify();
    }

    @Test
    void notFoundIsClassifiedAsEngineStateLostAndNeverRetried() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games/game-1/move")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.move("game-1", "X", 0))
                .isInstanceOf(EngineStateLostException.class);

        server.verify();
    }

    @Test
    void badRequestIsClassifiedAsEngineContractViolationAndNeverRetried() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games/game-1/move")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.move("game-1", "X", 0))
                .isInstanceOf(EngineContractViolationException.class);

        server.verify();
    }

    @Test
    void unparseableBodyIsClassifiedAsEngineBadResponse() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games"))
                .andRespond(withSuccess("not valid json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createGame("game-1"))
                .isInstanceOf(EngineBadResponseException.class);

        server.verify();
    }

    @Test
    void retryStopsAfterMaxAttemptsRatherThanRetryingForever() {
        GameEngineClientImpl client = clientWithRetries(2);
        server.expect(requestTo("http://engine/games")).andRespond(withServerError());
        server.expect(requestTo("http://engine/games")).andRespond(withServerError());

        assertThatThrownBy(() -> client.createGame("game-1"))
                .isInstanceOf(EngineUnavailableException.class);

        server.verify();
    }

    @Test
    void notFoundIsNotRetriedEvenOnceMoreThanTheSingleCall() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games/game-1/move")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.move("game-1", "X", 0)).isInstanceOf(EngineStateLostException.class);

        // Exactly one call was expected/consumed; a second would fail verify() with "no further
        // requests expected", proving classify()'s non-retryable branches short-circuit the retry.
        server.verify();
    }

    @Test
    void internalTokenHeaderIsPresentOnBothCreateGameAndMoveCalls() {
        GameEngineClientImpl client = clientWithRetries(3);
        server.expect(requestTo("http://engine/games"))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andRespond(withSuccess("""
                        {"board":[".",".",".",".",".",".",".",".","."]}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://engine/games/game-1/move"))
                .andExpect(header("X-Internal-Token", "test-internal-token"))
                .andRespond(withSuccess("""
                        {"board":["X",".",".",".",".",".",".",".","."],"gameState":"IN_PROGRESS","stepStatus":"CORRECT_STEP"}""",
                        MediaType.APPLICATION_JSON));

        client.createGame("game-1");
        client.move("game-1", "X", 0);

        server.verify();
    }
}
