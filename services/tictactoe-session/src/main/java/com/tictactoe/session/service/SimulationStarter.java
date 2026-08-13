package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.repository.SimulationJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SimulationStarter {

    private final SimulationJpaRepository simulationRepository;
    private final SimulationRunner runner;

    public SimulationStarter(SimulationJpaRepository simulationRepository, SimulationRunner runner) {
        this.simulationRepository = simulationRepository;
        this.runner = runner;
    }

    void start(String sessionId) {
        SimulationEntity simulation = new SimulationEntity();

        UUID simulationId = UUID.randomUUID();
        simulation.setId(simulationId);
        simulation.setSessionId(UUID.fromString(sessionId));
        simulation.setStatus(GameState.IN_PROGRESS);
        simulation.setStartedAt(Instant.now());

        simulationRepository.save(simulation);

        Thread.ofVirtual().start(() -> runner.run(sessionId, simulationId.toString()));
    }
}