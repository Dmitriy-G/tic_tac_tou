package com.tictactoe.session.service;

import com.tictactoe.session.domain.GameSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SessionService {

    GameSession createSession();

    GameSession simulate(String sessionId);

    GameSession getSession(String sessionId);

    SseEmitter subscribe(String sessionId);

    GameSession cancel(String sessionId);
}