package com.tictactoe.session.service;

import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.MoveStrategyResolver;
import com.tictactoe.session.strategy.MoveStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulationStep {

    private static final Logger log = LoggerFactory.getLogger(SimulationStep.class);

    private final GameEngineClient gameEngineClient;
    private final MoveStrategyResolver moveStrategyResolver;
    private final SimulationEventPublisher eventPublisher;

    public SimulationStep(GameEngineClient gameEngineClient,
                           MoveStrategyResolver moveStrategyResolver,
                           SimulationEventPublisher eventPublisher) {
        this.gameEngineClient = gameEngineClient;
        this.moveStrategyResolver = moveStrategyResolver;
        this.eventPublisher = eventPublisher;
    }

    void execute(String sessionId, String simulationId, SimulationProgress progress) {
        String symbol = progress.currentSymbol();
        MoveStrategy strategy = moveStrategyResolver.resolve(symbol);
        int position = strategy.selectMove(progress.board(), symbol);

        MoveResponse response = gameEngineClient.move(simulationId, symbol, position);

        eventPublisher.publishMove(sessionId, progress.moveNumber(), symbol, position, response);

        log.info("move={} symbol={} position={} status={}",
                progress.moveNumber(), symbol, position, response.gameState());

        progress.recordStep(response);
    }
}