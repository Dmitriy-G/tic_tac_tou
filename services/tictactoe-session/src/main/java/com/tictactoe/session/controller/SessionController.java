package com.tictactoe.session.controller;

import com.tictactoe.session.domain.GameSession;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sessions")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Sessions", description = "Session lifecycle and automated game simulation")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a session", description = "Creates a new game session, ready to be simulated.")
    public GameSession createSession() {
        return sessionService.createSession();
    }

    @PostMapping("/{sessionId}/simulate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Run a simulation", description = "Starts an automated game for the session; progress and outcome are delivered via the SSE stream.")
    public GameSession simulate(@Parameter(description = "Identifier of the session") @PathVariable String sessionId) {
        return sessionService.simulate(sessionId);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session state", description = "Returns the current status and move history for a session.")
    public GameSession getSession(@Parameter(description = "Identifier of the session") @PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @GetMapping(value = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to simulation events", description = "Server-Sent Events stream of board updates and status (in progress, win, or draw) for a session.")
    public SseEmitter events(@Parameter(description = "Identifier of the session") @PathVariable String sessionId) {
        return sessionService.subscribe(sessionId);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleSessionNotFound(SessionNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(SimulationAlreadyRunningException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleSimulationAlreadyRunning(SimulationAlreadyRunningException e) {
        return e.getMessage();
    }
}