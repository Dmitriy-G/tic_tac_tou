package com.tictactoe.session.controller;

import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@RestController
@RequestMapping("/sessions")
@Tag(name = "Sessions", description = "Session lifecycle and automated game simulation")
public class SessionController {

    public static final String OWNER_COOKIE = "tictactoe_session_owner";

    private final SessionService sessionService;
    private final boolean secureCookies;
    private final Duration ownerCookieMaxAge;

    public SessionController(SessionService sessionService,
                              @Value("${security.secure-cookies:false}") boolean secureCookies,
                              @Value("${security.owner-cookie-max-age:PT2H}") Duration ownerCookieMaxAge) {
        this.sessionService = sessionService;
        this.secureCookies = secureCookies;
        this.ownerCookieMaxAge = ownerCookieMaxAge;
    }

    @PostMapping
    @Operation(summary = "Create a session", description = "Creates a new game session, ready to be simulated. "
            + "The response sets an HttpOnly owner cookie required to start the simulation.")
    public ResponseEntity<SessionResponse> createSession() {
        SessionResponse response = sessionService.createSession();
        ResponseCookie cookie = ResponseCookie.from(OWNER_COOKIE, response.ownerToken())
                .httpOnly(true)
                .sameSite("Strict")
                .path("/api")
                .secure(secureCookies)
                .maxAge(ownerCookieMaxAge)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/{sessionId}/simulate")
    @Operation(summary = "Run a simulation", description = "Starts an automated game for the session; progress and outcome are delivered via the SSE stream. Requires the owner cookie from session creation.")
    public ResponseEntity<Void> simulate(
            @Parameter(description = "Identifier of the session") @PathVariable String sessionId,
            @CookieValue(name = OWNER_COOKIE, required = false) String ownerToken) {
        sessionService.simulate(sessionId, ownerToken);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session state", description = "Returns the current status and move history for a session.")
    public SessionResponse getSession(@Parameter(description = "Identifier of the session") @PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to simulation events", description = "Server-Sent Events stream of board updates and status (in progress, win, or draw) for a session.")
    public ResponseEntity<SseEmitter> events(
            @Parameter(description = "Identifier of the session") @PathVariable String sessionId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache")
                .body(sessionService.subscribe(sessionId, lastEventId));
    }
}