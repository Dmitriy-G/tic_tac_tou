package com.tictactoe.session.sse;

import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.session.domain.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Since {@code registry.register()} owns emitter creation, these tests intercept delivery by
 * having the registry's {@code SseEmitter.Builder} produce a recording subclass instead of a
 * plain {@code SseEmitter} — the only public seam {@link SseEmitter#send(SseEmitter.SseEventBuilder)}
 * offers without reaching into the registry's private state.
 */
class SseEmitterRegistryTest {

    private final SseEmitterRegistry registry = new SseEmitterRegistry(RecordingEmitter::new);

    @Test
    void aSecondSubscriberDoesNotEvictTheFirst() {
        RecordingEmitter first = (RecordingEmitter) registry.register("session-1");
        RecordingEmitter second = (RecordingEmitter) registry.register("session-1");

        registry.publish("session-1", moveEvent());

        assertThat(first.sendCount.get()).isEqualTo(1);
        assertThat(second.sendCount.get()).isEqualTo(1);
    }

    @Test
    void anIoExceptionFromOneEmitterDoesNotStopThePublishToOthers() {
        RecordingEmitter failing = (RecordingEmitter) registry.register("session-1");
        failing.failOnSend = true;
        RecordingEmitter healthy = (RecordingEmitter) registry.register("session-1");

        assertThatCode(() -> registry.publish("session-1", moveEvent())).doesNotThrowAnyException();

        assertThat(healthy.sendCount.get()).isEqualTo(1);
    }

    @Test
    void publishWithNoSubscribersDoesNotThrow() {
        assertThatCode(() -> registry.publish("unknown-session", moveEvent())).doesNotThrowAnyException();
    }

    @Test
    void completeReleasesAllSubscribersForTheSession() {
        RecordingEmitter emitter = (RecordingEmitter) registry.register("session-1");

        registry.complete("session-1");

        assertThat(emitter.completed.get()).isTrue();
    }

    @Test
    void publishingAFailureEventDoesNotThrow() {
        registry.register("session-1");

        assertThatCode(() -> registry.publish("session-1", failureEvent())).doesNotThrowAnyException();
    }

    private static SessionEvent moveEvent() {
        return new SessionEvent("session-1", SessionEvent.EventType.MOVE,
                List.of("X", "_", "_", "_", "_", "_", "_", "_", "_"), StepStatus.CORRECT_STEP, null, null, null, null);
    }

    private static SessionEvent failureEvent() {
        return new SessionEvent("session-1", SessionEvent.EventType.FAILED, null, null, null,
                "INTERNAL_ERROR", "boom", null);
    }

    private static class RecordingEmitter extends SseEmitter {

        private final AtomicInteger sendCount = new AtomicInteger();
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile boolean failOnSend = false;

        RecordingEmitter() {
            super(0L);
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failOnSend) {
                throw new IOException("broken pipe");
            }
            sendCount.incrementAndGet();
        }

        @Override
        public synchronized void complete() {
            completed.set(true);
        }
    }
}
