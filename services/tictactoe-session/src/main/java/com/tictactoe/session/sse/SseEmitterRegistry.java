package com.tictactoe.session.sse;

import com.tictactoe.session.domain.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Multiple browser tabs/subscribers can watch the same session, so each session id maps to a
 * list of emitters rather than one — a second {@code register} used to silently evict the first.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public SseEmitterRegistry() {
        this(() -> new SseEmitter(0L));
    }

    SseEmitterRegistry(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter register(String sessionId) {
        SseEmitter emitter = emitterFactory.get();
        List<SseEmitter> sessionEmitters = emitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>());
        sessionEmitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(throwable -> removeEmitter(sessionId, emitter));
        return emitter;
    }

    /**
     * A dead browser tab must not fail the simulation: each emitter is sent to independently, and
     * an {@link IOException} from one drops only that emitter, not the publish for the rest.
     */
    public void publish(String sessionId, SessionEvent event) {
        List<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) {
            log.debug("no subscribers for session {}, dropping event", sessionId);
            return;
        }
        String eventName = event.type() == SessionEvent.EventType.FAILED ? "failure" : "update";
        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(event, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                removeEmitter(sessionId, emitter);
            }
        }
    }

    public void complete(String sessionId) {
        List<SseEmitter> sessionEmitters = emitters.remove(sessionId);
        if (sessionEmitters == null) {
            return;
        }
        for (SseEmitter emitter : sessionEmitters) {
            emitter.complete();
        }
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        emitters.computeIfPresent(sessionId, (id, sessionEmitters) -> {
            sessionEmitters.remove(emitter);
            return sessionEmitters.isEmpty() ? null : sessionEmitters;
        });
    }
}
