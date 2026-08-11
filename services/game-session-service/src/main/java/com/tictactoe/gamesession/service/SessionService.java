package com.tictactoe.gamesession.service;

import com.tictactoe.gamesession.model.GameSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SessionService {

    GameSession createSession();

    GameSession simulate(String sessionId);

    GameSession getSession(String sessionId);

    SseEmitter subscribe(String sessionId);
}
