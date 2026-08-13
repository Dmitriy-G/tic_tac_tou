package com.tictactoe.session.service;

import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.session.client.GameEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulationRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunner.class);

    private final GameEngineClient gameEngineClient;
    private final SimulationStep step;
    private final SimulationStateWriter stateWriter;
    private final SimulationEventPublisher eventPublisher;

    public SimulationRunner(GameEngineClient gameEngineClient,
                             SimulationStep step,
                             SimulationStateWriter stateWriter,
                             SimulationEventPublisher eventPublisher) {
        this.gameEngineClient = gameEngineClient;
        this.step = step;
        this.stateWriter = stateWriter;
        this.eventPublisher = eventPublisher;
    }

    void run(String sessionId, String simulationId) {
        try {
            List<String> board = createGameOrFail(simulationId);
            SimulationProgress progress = SimulationProgress.start(board);
            while (progress.hasNextIteration()) {
                step.execute(sessionId, simulationId, progress);
                stateWriter.persist(simulationId, progress);
            }
        } catch (RuntimeException e) {
            log.warn("simulation {} failed: {}", simulationId, e.getMessage());
        } finally {
            eventPublisher.complete(sessionId);
        }
    }

    private List<String> createGameOrFail(String simulationId) {
        CreateGameResponse created = gameEngineClient.createGame(simulationId);
        if (created == null || created.board() == null) {
            throw new IllegalStateException("Engine returned an incomplete response for game creation");
        }
        return created.board();
    }
}