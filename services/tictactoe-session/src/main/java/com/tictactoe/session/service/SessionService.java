package com.tictactoe.session.service;

import com.tictactoe.session.dto.SessionDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SessionService {

    SessionDto createSession();

    void simulate(String sessionId);

    SessionDto getSession(String sessionId);

    SseEmitter subscribe(String sessionId);
}