package com.tictactoe.session.service;

import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.MoveStrategyResolver;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.repository.SimulationJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import com.tictactoe.session.strategy.MoveStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
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

    private static final int MAX_MOVES = 9;
    private static final int ERROR_COUNTS_THRESHOLD = 10;

    private final GameEngineClient gameEngineClient;
    private final SessionJpaRepository sessionRepository;
    private final SimulationJpaRepository simulationRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final MoveStrategyResolver moveStrategyResolver;

    public SessionServiceImpl(GameEngineClient gameEngineClient,
                              SessionJpaRepository sessionRepository,
                              SimulationJpaRepository simulationRepository,
                              SseEmitterRegistry emitterRegistry,
                              MoveStrategyResolver moveStrategyResolver) {
        this.gameEngineClient = gameEngineClient;
        this.sessionRepository = sessionRepository;
        this.simulationRepository = simulationRepository;
        this.emitterRegistry = emitterRegistry;
        this.moveStrategyResolver = moveStrategyResolver;
    }

    @Override
    public SessionResponse createSession() {
        UUID sessionId = UUID.randomUUID();

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        sessionRepository.save(session);

        return new SessionResponse(sessionId.toString());
    }

    @Override
    public void simulate(String sessionId) {
        SimulationEntity simulation = new SimulationEntity();

        UUID simulationId = UUID.randomUUID();
        simulation.setId(simulationId);
        simulation.setSessionId(UUID.fromString(sessionId));
        simulation.setStatus(GameState.IN_PROGRESS);
        simulation.setStartedAt(Instant.now());

        simulationRepository.save(simulation);

        Thread.ofVirtual().start(() -> runSimulation(sessionId, simulationId.toString()));
    }

    @Override
    public SessionResponse getSession(String sessionId) {
        return new SessionResponse(sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new SessionNotFoundException(sessionId)).getId().toString());
    }

    @Override
    public SseEmitter subscribe(String sessionId) {
        getSession(sessionId);
        return emitterRegistry.register(sessionId);
    }

    private void runSimulation(String sessionId, String simulationId) {

        int errorsCount = 0;

        try {
            CreateGameResponse created = gameEngineClient.createGame(simulationId);
            if (created == null || created.board() == null) {
                throw new IllegalStateException("Engine returned an incomplete response for game creation");
            }
            List<String> board = created.board();

            for (int moveNumber = 1; moveNumber <= MAX_MOVES; ) {

                String symbol = moveNumber % 2 == 1 ? "X" : "O";
                MoveStrategy strategy = moveStrategyResolver.resolve(symbol);
                int position = strategy.selectMove(board, symbol);

                MoveResponse response = gameEngineClient.move(simulationId, symbol, position);

                board = response.board();
                GameState gameState = response.gameState();

                // publish sse
                emitterRegistry.publish(sessionId, new SessionEvent(
                        sessionId,
                        new ArrayList<>(board),
                        response.stepStatus(),
                        gameState != GameState.IN_PROGRESS ? gameState.toString() : null));

                log.info("move={} symbol={} position={} status={}",
                        moveNumber, symbol, position, gameState);

                // increment counter
                if (!StepStatus.CORRECT_STEP.equals(response.stepStatus())) {
                    errorsCount++;
                    if (ERROR_COUNTS_THRESHOLD < errorsCount) {
                        gameState = GameState.FAILED;
                    }
                } else {
                    moveNumber++;
                }

                // persist
                SimulationEntity simulationEntity = simulationRepository.findById(UUID.fromString(simulationId)).orElseThrow();
                simulationEntity.setErrorsCount(errorsCount);
                simulationEntity.setStatus(gameState);
                simulationEntity.setFinishedAt(GameState.IN_PROGRESS.equals(gameState) ? null : Instant.now());
                simulationRepository.save(simulationEntity);

                if (!GameState.IN_PROGRESS.equals(gameState)) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            log.warn("simulation {} failed: {}", simulationId, e.getMessage());
        } finally {
            emitterRegistry.complete(sessionId);
        }
    }
}