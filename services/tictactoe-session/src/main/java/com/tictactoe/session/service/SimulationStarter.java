package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.config.CorrelationIdFilter;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.exception.SessionAlreadyCompletedException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.util.SessionIdUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SimulationStarter {

    private final SessionJpaRepository sessionRepository;
    private final SimulationRunner runner;

    public SimulationStarter(SessionJpaRepository sessionRepository, SimulationRunner runner) {
        this.sessionRepository = sessionRepository;
        this.runner = runner;
    }

    void start(String sessionId) {
        UUID id = SessionIdUtils.toUuid(sessionId);
        SessionEntity session = sessionRepository.findById(id)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (sessionRepository.claimForSimulation(id, Instant.now()) == 0) {
            // The CAS lost, so the status is no longer CREATED. Distinguish the two reasons
            // for the caller — the read above is only for the error message, never for the guard:
            // two concurrent callers produce exactly one 1 and one 0 with no window in between.
            throw session.getStatus() == GameState.IN_PROGRESS
                    ? new SimulationAlreadyRunningException(sessionId)
                    : new SessionAlreadyCompletedException(sessionId);
        }

        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        String effectiveTraceId = traceId != null ? traceId : sessionId;
        Thread.ofVirtual().start(() -> runner.run(sessionId, effectiveTraceId));
    }
}