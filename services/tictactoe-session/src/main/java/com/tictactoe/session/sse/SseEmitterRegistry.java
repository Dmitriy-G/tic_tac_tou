package com.tictactoe.session.sse;

import com.tictactoe.session.domain.SessionEvent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String sessionId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId, emitter));
        emitter.onTimeout(() -> emitters.remove(sessionId, emitter));
        emitter.onError(throwable -> emitters.remove(sessionId, emitter));
        return emitter;
    }

    public void publish(String sessionId, SessionEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("update").data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(sessionId, emitter);
        }
    }

    public void complete(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}