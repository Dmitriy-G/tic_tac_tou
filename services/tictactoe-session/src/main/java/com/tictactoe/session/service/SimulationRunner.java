package com.tictactoe.session.service;

import com.tictactoe.common.error.BaseException;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.CorrelationIdFilter;
import com.tictactoe.session.dto.MoveRecord;
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
    private final SessionStateWriter stateWriter;
    private final SimulationEventPublisher eventPublisher;

    public SimulationRunner(GameEngineClient gameEngineClient,
                             SimulationStep step,
                             SessionStateWriter stateWriter,
                             SimulationEventPublisher eventPublisher) {
        this.gameEngineClient = gameEngineClient;
        this.step = step;
        this.stateWriter = stateWriter;
        this.eventPublisher = eventPublisher;
    }

    /** {@code sessionId} is the engine's {@code gameId} — one session is one game, so a session
     * can only ever be simulated once and a gameId collision at the engine is impossible by
     * construction. */
    void run(String sessionId, String traceId) {
        try {
            MDC.put(CorrelationIdFilter.MDC_KEY, traceId);
            List<String> board = gameEngineClient.createGame(sessionId).board();
            SimulationProgress progress = SimulationProgress.start(board);
            while (progress.hasNextIteration()) {
                MoveRecord move = step.execute(sessionId, progress);
                stateWriter.recordMove(sessionId, progress, move);
            }
            stateWriter.persistTerminal(sessionId, progress);
        } catch (RuntimeException e) {
            ErrorCode code = codeOf(e);
            log.error("simulation {} failed code={}", sessionId, code.getCode(), e);
            stateWriter.fail(sessionId, code, e.getMessage());
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