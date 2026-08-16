package com.tictactoe.engine.controller;

import com.tictactoe.engine.config.InternalTokenFilter;
import com.tictactoe.engine.repository.GameJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The engine's fault channel end to end: every rejection here is a broken/malformed request or
 * an unknown game, never a rule outcome — see docs for the
 * distinction. {@link GameJpaRepository} is mocked because these tests exercise translation, not
 * persistence (already covered by GameStoreTest / GameRepositoryTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerErrorHandlingTest {

    private static final String TEST_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameJpaRepository gameRepository;

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder) {
        return builder.header(InternalTokenFilter.HEADER, TEST_TOKEN);
    }

    @Test
    void unknownGameIdReturns404GameNotFound() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        mockMvc.perform(authed(get("/games/{gameId}", gameId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void nonUuidGameIdReturns400InvalidGameId() throws Exception {
        mockMvc.perform(authed(get("/games/{gameId}", "not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GAME_ID"));
    }

    @Test
    void malformedJsonBodyReturns400MalformedRequest() throws Exception {
        mockMvc.perform(authed(post("/games/{gameId}/move", UUID.randomUUID()))
                        .contentType("application/json")
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void positionOutOfRangeReturns400ValidationErrorWithAFieldError() throws Exception {
        mockMvc.perform(authed(post("/games/{gameId}/move", UUID.randomUUID()))
                        .contentType("application/json")
                        .content("{\"symbol\":\"X\",\"position\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("position"));
    }

    @Test
    void nullSymbolReturns400ValidationError() throws Exception {
        mockMvc.perform(authed(post("/games/{gameId}/move", UUID.randomUUID()))
                        .contentType("application/json")
                        .content("{\"symbol\":null,\"position\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getOnAPostOnlyPathReturns405() throws Exception {
        mockMvc.perform(authed(get("/games/{gameId}/move", UUID.randomUUID())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void repositoryFailureReturns503DatabaseError() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(any())).thenThrow(new DataAccessResourceFailureException("db unreachable"));

        mockMvc.perform(authed(get("/games/{gameId}", gameId)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_ERROR"));
    }

    @Test
    void everyErrorResponseCarriesTheFullShape() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        mockMvc.perform(authed(get("/games/{gameId}", gameId)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/games/" + gameId))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
