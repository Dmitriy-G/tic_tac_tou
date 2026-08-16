package com.tictactoe.engine.controller;

import com.tictactoe.engine.config.InternalTokenFilter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The happy path through the engine's HTTP layer, against the real service and the H2 test
 * database — no mocks. {@link GameControllerErrorHandlingTest} and {@link GameControllerSecurityTest}
 * cover the rejection paths; this class only covers requests that succeed, including the wire-shape
 * inconsistency pinned by {@link #createGameResponseUsesAListOfNineEmptyCells()} and
 * {@link #moveResponseBoardIsAlsoAList()} versus {@link #getGameReturnsTheCurrentBoardAndState()}:
 * {@code GameResponse.board} is a single {@code String} while {@code CreateGameResponse.board} and
 * {@code MoveResponse.board} are a {@code List<String>}. That inconsistency is real and pinned
 * here deliberately, not "fixed" by this test suite.
 *
 * <p>{@code POST /games} takes the caller's own game id ({@code CreateGameRequest.gameId}) rather
 * than generating and returning one — {@code CreateGameResponse} carries no id field. That matches
 * how the session service actually calls it: {@code sessionId} doubles as {@code gameId}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerTest {

    private static final String TEST_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header(InternalTokenFilter.HEADER, TEST_TOKEN);
    }

    @Test
    void createGameResponseUsesAListOfNineEmptyCells() throws Exception {
        mockMvc.perform(authed(post("/games")).contentType("application/json").content(createRequest(newGameId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.board", Matchers.hasSize(9)))
                .andExpect(jsonPath("$.board[0]").value("."));
    }

    @Test
    void aLegalMoveReturns200WithTheUpdatedBoardAndInProgressStatus() throws Exception {
        String gameId = createGame();

        mockMvc.perform(authed(post("/games/{gameId}/move", gameId))
                        .contentType("application/json")
                        .content("{\"symbol\":\"X\",\"position\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board[4]").value("X"))
                .andExpect(jsonPath("$.stepStatus").value("CORRECT_STEP"))
                .andExpect(jsonPath("$.gameState").value("IN_PROGRESS"));
    }

    @Test
    void moveResponseBoardIsAlsoAList() throws Exception {
        String gameId = createGame();

        mockMvc.perform(authed(post("/games/{gameId}/move", gameId))
                        .contentType("application/json")
                        .content("{\"symbol\":\"X\",\"position\":0}"))
                .andExpect(jsonPath("$.board", Matchers.hasSize(9)));
    }

    @Test
    void aWinningMoveReturns200WithXWonAndTheWinner() throws Exception {
        String gameId = createGame();

        move(gameId, "X", 0);
        move(gameId, "O", 3);
        move(gameId, "X", 1);
        move(gameId, "O", 4);

        mockMvc.perform(authed(post("/games/{gameId}/move", gameId))
                        .contentType("application/json")
                        .content("{\"symbol\":\"X\",\"position\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameState").value("X_WON"))
                .andExpect(jsonPath("$.winner").value("X"));
    }

    @Test
    void getGameReturnsTheCurrentBoardAndState() throws Exception {
        String gameId = createGame();
        move(gameId, "X", 0);

        mockMvc.perform(authed(get("/games/{gameId}", gameId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board").value("X........"))
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
    }

    private String createGame() throws Exception {
        String gameId = newGameId();
        mockMvc.perform(authed(post("/games")).contentType("application/json").content(createRequest(gameId)))
                .andExpect(status().isCreated());
        return gameId;
    }

    private void move(String gameId, String symbol, int position) throws Exception {
        mockMvc.perform(authed(post("/games/{gameId}/move", gameId))
                        .contentType("application/json")
                        .content("{\"symbol\":\"" + symbol + "\",\"position\":" + position + "}"))
                .andExpect(status().isOk());
    }

    private static String newGameId() {
        return UUID.randomUUID().toString();
    }

    private static String createRequest(String gameId) {
        return "{\"gameId\":\"" + gameId + "\"}";
    }
}
