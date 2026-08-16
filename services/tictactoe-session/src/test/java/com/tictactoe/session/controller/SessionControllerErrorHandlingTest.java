package com.tictactoe.session.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictactoe.session.client.GameEngineClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The session service's fault channel: unknown sessions and the double-simulate conflict.
 * {@link GameEngineClient} is mocked to fail fast so the background simulation thread each
 * {@code /simulate} call spawns doesn't hang the test suite on a real HTTP call to a
 * non-existent engine. Ownership enforcement lives in {@code SessionControllerSecurityTest} and
 * the concurrent-simulate race lives in {@code SessionControllerConcurrencyTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerErrorHandlingTest {

    private static final String OWNER_COOKIE = "tictactoe_session_owner";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameEngineClient gameEngineClient;

    @Test
    void unknownSessionIdOnGetReturns404SessionNotFound() throws Exception {
        mockMvc.perform(get("/sessions/{sessionId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void unknownSessionIdOnSimulateReturns404SessionNotFoundEvenWithNoCookie() throws Exception {
        mockMvc.perform(post("/sessions/{sessionId}/simulate", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void unknownSessionIdOnEventsReturns404SessionNotFound() throws Exception {
        mockMvc.perform(get("/sessions/{sessionId}/events", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void malformedSessionIdOnGetReturns400InvalidSessionId() throws Exception {
        mockMvc.perform(get("/sessions/{sessionId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SESSION_ID"));
    }

    @Test
    void malformedSessionIdOnSimulateReturns400InvalidSessionId() throws Exception {
        mockMvc.perform(post("/sessions/{sessionId}/simulate", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SESSION_ID"));
    }

    @Test
    void secondSimulateWhileRunningReturns409SimulationAlreadyRunning() throws Exception {
        when(gameEngineClient.createGame(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("engine unreachable"));
        CreatedSession session = createSession();

        mockMvc.perform(post("/sessions/{sessionId}/simulate", session.id()).cookie(session.ownerCookie()))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/sessions/{sessionId}/simulate", session.id()).cookie(session.ownerCookie()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIMULATION_ALREADY_RUNNING"));
    }

    private CreatedSession createSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sessionId").asText();
        Cookie ownerCookie = result.getResponse().getCookie(OWNER_COOKIE);
        return new CreatedSession(sessionId, ownerCookie);
    }

    private record CreatedSession(String id, Cookie ownerCookie) {
    }
}
