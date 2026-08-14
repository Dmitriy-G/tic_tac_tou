package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.config.CorrelationIdFilter;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SimulationJpaRepository;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SimulationStarter {

    private final SessionJpaRepository sessionRepository;
    private final SimulationJpaRepository simulationRepository;
    private final SimulationRunner runner;
    private final SessionStateStore stateStore;

    public SimulationStarter(SessionJpaRepository sessionRepository,
                              SimulationJpaRepository simulationRepository,
                              SimulationRunner runner,
                              SessionStateStore stateStore) {
        this.sessionRepository = sessionRepository;
        this.simulationRepository = simulationRepository;
        this.runner = runner;
        this.stateStore = stateStore;
    }

    void start(String sessionId) {
        UUID sessionUuid = UUID.fromString(sessionId);

        if (!sessionRepository.existsById(sessionUuid)) {
            throw new SessionNotFoundException(sessionId);
        }
        if (simulationRepository.existsBySessionIdAndStatus(sessionUuid, GameState.IN_PROGRESS)) {
            throw new SimulationAlreadyRunningException(sessionId);
        }

        SimulationEntity simulation = new SimulationEntity();
        UUID simulationId = UUID.randomUUID();
        simulation.setId(simulationId);
        simulation.setSessionId(sessionUuid);
        simulation.setStatus(GameState.IN_PROGRESS);
        simulation.setRunningSessionId(sessionUuid);
        simulation.setStartedAt(Instant.now());

        try {
            simulationRepository.save(simulation);
        } catch (DataIntegrityViolationException e) {
            throw new SimulationAlreadyRunningException(sessionId);
        }

        stateStore.markRunning(sessionId);

        String traceId = MDC.get(CorrelationIdFilter.MDC_KEY);
        String effectiveTraceId = traceId != null ? traceId : sessionId;
        Thread.ofVirtual().start(() -> runner.run(sessionId, simulationId.toString(), effectiveTraceId));
    }
}
