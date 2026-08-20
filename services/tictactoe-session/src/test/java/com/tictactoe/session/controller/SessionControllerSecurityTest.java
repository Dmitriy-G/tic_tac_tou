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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Session-owner-token enforcement, split out of {@code SessionControllerErrorHandlingTest} which
 * is about unknown/malformed-id rejections, not auth. {@link GameEngineClient} is mocked purely so
 * session creation doesn't need a real engine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionControllerSecurityTest {

    private static final String OWNER_COOKIE = "tictactoe_session_owner";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameEngineClient gameEngineClient;

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
