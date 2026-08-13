package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SessionService {

    SessionResponse createSession();

    void simulate(String sessionId);

    SessionResponse getSession(String sessionId);

    SseEmitter subscribe(String sessionId);
}