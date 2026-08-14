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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The session service's fault channel: unknown sessions, the double-simulate conflict, and
 * session-ownership enforcement on {@code /simulate}. {@link GameEngineClient} is mocked to fail
 * fast so the background simulation thread each {@code /simulate} call spawns doesn't hang the
 * test suite on a real HTTP call to a non-existent engine.
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
    void simulateWithoutOwnerCookieReturns403NotSessionOwner() throws Exception {
        CreatedSession session = createSession();

        mockMvc.perform(post("/sessions/{sessionId}/simulate", session.id()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_SESSION_OWNER"));
    }

    @Test
    void simulateWithAnotherSessionsOwnerCookieReturns403NotSessionOwner() throws Exception {
        CreatedSession session = createSession();
        CreatedSession otherSession = createSession();

        mockMvc.perform(post("/sessions/{sessionId}/simulate", session.id()).cookie(otherSession.ownerCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_SESSION_OWNER"));
    }

    @Test
    void createSessionSetsAnHttpOnlyStrictSameSiteOwnerCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/sessions"))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists(OWNER_COOKIE))
                .andExpect(cookie().httpOnly(OWNER_COOKIE, true))
                .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
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

    @Test
    void concurrentSimulateCallsResultInExactlyOne202AndOne409() throws Exception {
        when(gameEngineClient.createGame(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("engine unreachable"));
        CreatedSession session = createSession();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> call = () -> {
            ready.countDown();
            start.await();
            MvcResult result = mockMvc.perform(
                            post("/sessions/{sessionId}/simulate", session.id()).cookie(session.ownerCookie()))
                    .andReturn();
            return result.getResponse().getStatus();
        };

        try {
            Future<Integer> first = executor.submit(call);
            Future<Integer> second = executor.submit(call);
            ready.await();
            start.countDown();

            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses).containsExactlyInAnyOrder(202, 409);
        } finally {
            executor.shutdown();
        }
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
