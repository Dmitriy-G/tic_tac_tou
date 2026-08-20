package com.tictactoe.session.controller;

import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The happy path through the session service's HTTP layer. Ownership/security and the
 * double-simulate conflict are covered by {@code SessionControllerSecurityTest},
 * {@code SessionControllerConcurrencyTest} and {@code SessionControllerErrorHandlingTest}; this
 * class only covers requests that succeed. {@link GameEngineClient} is mocked since none of these
 * requests reach the engine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseEmitterRegistry emitterRegistry;

    @MockBean
    private GameEngineClient gameEngineClient;

    @Test
    void createSessionReturns201WithAUuidSessionIdAndAnOwnerToken() throws Exception {
        mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))
                .andExpect(jsonPath("$.ownerToken").isNotEmpty());
    }

    @Test
    void freshSessionIsCreatedWithAnEmptyBoardAndNoMoves() throws Exception {
        String sessionId = createSession();

        mockMvc.perform(get("/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.board", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("."))))
                .andExpect(jsonPath("$.moves", org.hamcrest.Matchers.empty()));
    }

    @Test
    void eventsStreamRespondsWithSseContentTypeAndNoBufferingHeader() throws Exception {
        String sessionId = createSession();

        MvcResult result = mockMvc.perform(get("/sessions/{sessionId}/events", sessionId))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();
        // A real SSE stream never completes on its own; complete it explicitly so MockMvc's async
        // machinery has a terminal result to dispatch and inspect final headers on.
        emitterRegistry.complete(sessionId);
        result.getAsyncResult(5000);

        mockMvc.perform(asyncDispatch(result))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(header().string("X-Accel-Buffering", "no"));
    }

    private String createSession() throws Exception {
        String body = mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("sessionId").asText();
    }
}
