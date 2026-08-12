package com.tictactoe.session.service;

import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.client.GameEngineResponse;
import com.tictactoe.session.config.MoveStrategyResolver;
import com.tictactoe.session.domain.GameSession;
import com.tictactoe.session.domain.MoveRecord;
import com.tictactoe.session.domain.Simulation;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.domain.SessionStatus;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.repository.SessionRepository;
import com.tictactoe.session.repository.SimulationRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import com.tictactoe.session.strategy.MoveStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Drives an automated game end-to-end: creates the game at the engine, then alternates X/O moves
 * (each move's cell chosen by the {@link MoveStrategy} configured for that symbol) until the
 * engine reports a terminal status, publishing an SSE event after every move.
 */
@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionServiceImpl.class);

    private static final int BOARD_SIZE = 9;
    private static final int MAX_MOVES = 9;

    private final GameEngineClient gameEngineClient;
    private final SessionRepository sessionRepository;
    private final SimulationRepository simulationRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final MoveStrategyResolver moveStrategyResolver;
    private final long moveDelayMs;

    public SessionServiceImpl(GameEngineClient gameEngineClient,
                               SessionRepository sessionRepository,
                               SimulationRepository simulationRepository,
                               SseEmitterRegistry emitterRegistry,
                               MoveStrategyResolver moveStrategyResolver,
                               @Value("${simulation.move-delay-ms:500}") long moveDelayMs) {
        this.gameEngineClient = gameEngineClient;
        this.sessionRepository = sessionRepository;
        this.simulationRepository = simulationRepository;
        this.emitterRegistry = emitterRegistry;
        this.moveStrategyResolver = moveStrategyResolver;
        this.moveDelayMs = moveDelayMs;
    }

    @Override
    public GameSession createSession() {
        GameSession session = new GameSession();
        String sessionId = UUID.randomUUID().toString();
        session.setSessionId(sessionId);
        session.setGameId(sessionId);
        session.setStatus(SessionStatus.CREATED);
        session.setMoveHistory(new ArrayList<>());
        return sessionRepository.save(session);
    }

    @Override
    public GameSession simulate(String sessionId) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() == SessionStatus.IN_PROGRESS || isTerminal(session.getStatus())) {
            throw new SimulationAlreadyRunningException(sessionId);
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setMoveHistory(new ArrayList<>());
        sessionRepository.save(session);

        Simulation simulation = new Simulation();
        simulation.setId(UUID.randomUUID().toString());
        simulation.setSessionId(sessionId);
        simulation.setErrorsCount(0);
        simulation.setStartedAt(Instant.now());
        simulation.setStatus(SessionStatus.IN_PROGRESS);
        simulationRepository.save(simulation);

        Thread.ofVirtual().start(() -> runSimulation(session, simulation));

        return session;
    }

    @Override
    public GameSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Override
    public SseEmitter subscribe(String sessionId) {
        getSession(sessionId);
        return emitterRegistry.register(sessionId);
    }

    private void runSimulation(GameSession session, Simulation simulation) {
        String sessionId = session.getSessionId();
        String gameId = session.getGameId();
        int errorsCount = 0;

        try {
            gameEngineClient.createGame(gameId);

            List<String> board = new ArrayList<>(Collections.nCopies(BOARD_SIZE, null));
            for (int moveNumber = 1; moveNumber <= MAX_MOVES; moveNumber++) {
                if (!sleepInterruptibly(moveDelayMs)) {
                    break;
                }

                String symbol = moveNumber % 2 == 1 ? "X" : "O";
                MoveStrategy strategy = moveStrategyResolver.resolve(symbol);
                int position = strategy.selectMove(board, symbol);

                GameEngineResponse response = gameEngineClient.move(gameId, symbol, position);
                if (response == null || response.getBoard() == null || response.getStatus() == null) {
                    throw new IllegalStateException("Engine returned an incomplete response for move " + moveNumber);
                }
                board = response.getBoard();

                MoveRecord record = new MoveRecord();
                record.setPlayer(symbol);
                record.setPosition(position);
                session.getMoveHistory().add(record);

                SessionStatus status = mapStatus(response.getStatus());
                session.setStatus(status);

                log.info("session={} move={} symbol={} position={} status={}",
                        sessionId, moveNumber, symbol, position, status);

                emitterRegistry.publish(sessionId, new SessionEvent(sessionId, new ArrayList<>(board), status,
                        status != SessionStatus.IN_PROGRESS ? response.getWinner() : null));

                if (status != SessionStatus.IN_PROGRESS) {
                    break;
                }
            }

            if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                // Hard cap reached (or interrupted) without a terminal outcome from the engine.
                session.setStatus(SessionStatus.FAILED);
                errorsCount++;
            }
        } catch (RuntimeException e) {
            log.warn("session={} simulation failed: {}", sessionId, e.getMessage());
            session.setStatus(SessionStatus.FAILED);
            errorsCount++;
        } finally {
            emitterRegistry.complete(sessionId);
            simulation.setErrorsCount(errorsCount);
            simulation.setStatus(session.getStatus());
            simulation.setFinishedAt(Instant.now());
            simulationRepository.save(simulation);
        }
    }

    private static SessionStatus mapStatus(String engineStatus) {
        return switch (engineStatus) {
            case "IN_PROGRESS" -> SessionStatus.IN_PROGRESS;
            case "X_WON" -> SessionStatus.X_WON;
            case "O_WON" -> SessionStatus.O_WON;
            case "DRAW" -> SessionStatus.DRAW;
            default -> throw new IllegalStateException("Unknown engine status: " + engineStatus);
        };
    }

    private static boolean isTerminal(SessionStatus status) {
        return status == SessionStatus.X_WON
                || status == SessionStatus.O_WON
                || status == SessionStatus.DRAW
                || status == SessionStatus.FAILED
                || status == SessionStatus.CANCELLED;
    }

    private boolean sleepInterruptibly(long millis) {
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}