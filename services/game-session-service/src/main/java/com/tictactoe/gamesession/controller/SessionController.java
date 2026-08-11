package com.tictactoe.gamesession.controller;

import com.tictactoe.gamesession.model.GameSession;
import com.tictactoe.gamesession.service.SessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
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
}
