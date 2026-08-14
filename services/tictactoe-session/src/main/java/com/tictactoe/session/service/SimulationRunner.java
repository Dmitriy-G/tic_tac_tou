package com.tictactoe.session.service;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    void run(String sessionId, String simulationId, String traceId) {
        try {
            MDC.put(CorrelationIdFilter.MDC_KEY, traceId);
            List<String> board = gameEngineClient.createGame(simulationId).board();
            SimulationProgress progress = SimulationProgress.start(board);
            while (progress.hasNextIteration()) {
                step.execute(sessionId, simulationId, progress);
                stateWriter.persist(simulationId, progress);
            }
            stateWriter.persistTerminal(simulationId, progress);
        } catch (RuntimeException e) {
            ErrorCode code = codeOf(e);
            log.error("simulation {} failed code={}", simulationId, code.getCode(), e);
            stateWriter.fail(simulationId, code, e.getMessage());
            eventPublisher.publishFailure(sessionId, code, e.getMessage());
        } finally {
            eventPublisher.complete(sessionId);
            MDC.clear();
        }
    }

    private static ErrorCode codeOf(RuntimeException e) {
        return e instanceof BaseException baseException ? baseException.getErrorCode() : ErrorCode.INTERNAL_ERROR;
    }
}
