package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.repository.SimulationJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SimulationStateWriter {

    private final SimulationJpaRepository simulationRepository;

    public SimulationStateWriter(SimulationJpaRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    void persist(String simulationId, SimulationProgress progress) {
        SimulationEntity simulationEntity = simulationRepository.findById(UUID.fromString(simulationId)).orElseThrow();
        simulationEntity.setErrorsCount(progress.errorsCount());
        simulationEntity.setStatus(progress.gameState());
        simulationEntity.setFinishedAt(GameState.IN_PROGRESS.equals(progress.gameState()) ? null : Instant.now());
        simulationRepository.save(simulationEntity);
    }
}