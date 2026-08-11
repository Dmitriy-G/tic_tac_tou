package com.tictactoe.gamesession.controller;

import com.tictactoe.gamesession.exception.SessionNotFoundException;
import com.tictactoe.gamesession.model.GameSession;
import com.tictactoe.gamesession.service.SessionService;
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
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public GameSession createSession() {
        return sessionService.createSession();
    }

    @PostMapping("/{sessionId}/simulate")
    public GameSession simulate(@PathVariable String sessionId) {
        return sessionService.simulate(sessionId);
    }

    @GetMapping("/{sessionId}")
    public GameSession getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @GetMapping(value = "/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String sessionId) {
        return sessionService.subscribe(sessionId);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleSessionNotFound(SessionNotFoundException e) {
        return e.getMessage();
    }
}