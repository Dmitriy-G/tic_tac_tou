package com.tictactoe.engine.controller;

import com.tictactoe.engine.config.InternalTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Internal-token enforcement on the engine's endpoints, split out of
 * {@link GameControllerErrorHandlingTest} which is about request/state rejections, not auth.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingInternalTokenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/games/{gameId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void wrongInternalTokenReturns401Unauthorized() throws Exception {
        mockMvc.perform(get("/games/{gameId}", UUID.randomUUID())
                        .header(InternalTokenFilter.HEADER, "not-the-right-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void actuatorHealthRequiresNoInternalToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}