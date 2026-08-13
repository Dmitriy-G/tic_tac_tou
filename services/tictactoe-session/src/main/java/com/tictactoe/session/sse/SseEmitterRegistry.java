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

    public SseEmitter register(String simulationId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(simulationId, emitter);
        emitter.onCompletion(() -> emitters.remove(simulationId, emitter));
        emitter.onTimeout(() -> emitters.remove(simulationId, emitter));
        emitter.onError(throwable -> emitters.remove(simulationId, emitter));
        return emitter;
    }

    public void publish(String sessionId, SessionEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        String eventName = "simulate";
        if (emitter == null) {
            //TODO: logging
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(eventName).data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(sessionId, emitter);
        }
    }

    public void complete(String simulationId) {
        SseEmitter emitter = emitters.remove(simulationId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}