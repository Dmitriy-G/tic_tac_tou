package com.tictactoe.session.sse;

import com.tictactoe.session.domain.SessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private static final int MAX_BACKLOG_PER_SESSION = 32;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, List<BacklogEntry>> backlogs = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public SseEmitterRegistry() {
        this(() -> new SseEmitter(0L));
    }

    SseEmitterRegistry(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public SseEmitter register(String sessionId, String lastEventId) {
        SseEmitter emitter = emitterFactory.get();
        List<SseEmitter> sessionEmitters = emitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>());
        sessionEmitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(throwable -> removeEmitter(sessionId, emitter));

        replayBacklog(sessionId, lastEventId, emitter);
        return emitter;
    }

    public void publish(String sessionId, SessionEvent event) {
        long id = sequences.computeIfAbsent(sessionId, key -> new AtomicLong()).incrementAndGet();
        String eventName = eventNameFor(event);
        addToBacklog(sessionId, new BacklogEntry(id, eventName, event));

        List<SseEmitter> sessionEmitters = emitters.get(sessionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) {
            log.debug("no subscribers for session {}, dropping event", sessionId);
            return;
        }
        for (SseEmitter emitter : sessionEmitters) {
            sendQuietly(sessionId, emitter, id, eventName, event);
        }
    }

    public void complete(String sessionId) {
        backlogs.remove(sessionId);
        sequences.remove(sessionId);
        List<SseEmitter> sessionEmitters = emitters.remove(sessionId);
        if (sessionEmitters == null) {
            return;
        }
        for (SseEmitter emitter : sessionEmitters) {
            emitter.complete();
        }
    }

    @Scheduled(fixedDelayString = "${sse.heartbeat-interval-ms:15000}")
    void heartbeat() {
        emitters.forEach((sessionId, sessionEmitters) -> {
            for (SseEmitter emitter : sessionEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    removeEmitter(sessionId, emitter);
                }
            }
        });
    }

    private void replayBacklog(String sessionId, String lastEventId, SseEmitter emitter) {
        List<BacklogEntry> sessionBacklog = backlogs.get(sessionId);
        if (sessionBacklog == null || sessionBacklog.isEmpty()) {
            return;
        }
        long afterId = parseLastEventId(lastEventId);
        for (BacklogEntry entry : sessionBacklog) {
            if (entry.id() > afterId) {
                sendQuietly(sessionId, emitter, entry.id(), entry.eventName(), entry.event());
            }
        }
    }

    private static long parseLastEventId(String lastEventId) {
        if (lastEventId == null) {
            return 0L;
        }
        try {
            return Long.parseLong(lastEventId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void addToBacklog(String sessionId, BacklogEntry entry) {
        List<BacklogEntry> sessionBacklog = backlogs.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>());
        sessionBacklog.add(entry);
        while (sessionBacklog.size() > MAX_BACKLOG_PER_SESSION) {
            sessionBacklog.remove(0);
        }
    }

    private void sendQuietly(String sessionId, SseEmitter emitter, long id, String eventName, SessionEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(id))
                    .reconnectTime(2000)
                    .name(eventName)
                    .data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            removeEmitter(sessionId, emitter);
        }
    }

    private static String eventNameFor(SessionEvent event) {
        return event.type() == SessionEvent.EventType.FAILED ? "failure" : "update";
    }

    private void removeEmitter(String sessionId, SseEmitter emitter) {
        emitters.computeIfPresent(sessionId, (id, sessionEmitters) -> {
            sessionEmitters.remove(emitter);
            return sessionEmitters.isEmpty() ? null : sessionEmitters;
        });
    }

    private record BacklogEntry(long id, String eventName, SessionEvent event) {
    }
}
