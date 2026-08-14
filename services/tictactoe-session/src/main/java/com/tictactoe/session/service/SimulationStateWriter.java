package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.InternalException;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.repository.SimulationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class SimulationStateWriter {

    private static final Logger log = LoggerFactory.getLogger(SimulationStateWriter.class);

    private final SimulationJpaRepository simulationRepository;

    public SimulationStateWriter(SimulationJpaRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    void persist(String simulationId, SimulationProgress progress) {
        SimulationEntity simulationEntity = findOrThrow(simulationId);
        boolean stillRunning = GameState.IN_PROGRESS.equals(progress.gameState());
        simulationEntity.setErrorsCount(progress.errorsCount());
        simulationEntity.setStatus(progress.gameState());
        simulationEntity.setFinishedAt(stillRunning ? null : Instant.now());
        if (!stillRunning) {
            simulationEntity.setRunningSessionId(null);
        }
        simulationRepository.save(simulationEntity);
    }

    void persistTerminal(String simulationId, SimulationProgress progress) {
        if (GameState.IN_PROGRESS.equals(progress.gameState())) {
            fail(simulationId, ErrorCode.INTERNAL_ERROR,
                    "Simulation exhausted its move budget without the engine reaching a terminal state");
        }
    }

    void fail(String simulationId, ErrorCode errorCode, String message) {
        try {
            SimulationEntity simulationEntity = findOrThrow(simulationId);
            simulationEntity.setStatus(GameState.FAILED);
            simulationEntity.setFinishedAt(Instant.now());
            simulationEntity.setRunningSessionId(null);
            simulationEntity.setErrorCode(errorCode.getCode());
            simulationEntity.setErrorMessage(message);
            simulationRepository.save(simulationEntity);
        } catch (RuntimeException e) {
            log.error("failed to persist FAILED state for simulation {}", simulationId, e);
        }
    }

    private SimulationEntity findOrThrow(String simulationId) {
        return simulationRepository.findById(UUID.fromString(simulationId))
                .orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR,
                        "Simulation " + simulationId + " vanished mid-run"));
    }
}
