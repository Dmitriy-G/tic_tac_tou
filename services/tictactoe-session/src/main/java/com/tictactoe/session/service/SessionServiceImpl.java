package com.tictactoe.session.service;

import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.domain.GameSession;
import com.tictactoe.session.domain.MoveRecord;
import com.tictactoe.session.domain.Simulation;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.domain.SessionStatus;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionRepository;
import com.tictactoe.session.repository.SimulationRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The real simulation would delegate move generation and rule enforcement to
 * {@link GameEngineClient}, which is still a skeleton. Until that is implemented, this class
 * drives {@link #simulate(String)} with a couple of scripted move sequences so the SSE pipeline
 * end-to-end (session -> events -> client) can be built and demoed.
 */
@Service
public class SessionServiceImpl implements SessionService {

    private static final int BOARD_SIZE = 9;
    private static final long MOVE_DELAY_MS = 700;

    private static final String[] WIN_SCRIPT_PLAYERS = {"X", "O", "X", "O", "X"};
    private static final int[] WIN_SCRIPT_POSITIONS = {0, 3, 1, 4, 2};
    private static final String WIN_SCRIPT_WINNER = "X";

    private static final String[] DRAW_SCRIPT_PLAYERS = {"X", "O", "X", "O", "X", "O", "X", "O", "X"};
    private static final int[] DRAW_SCRIPT_POSITIONS = {0, 1, 2, 4, 3, 5, 7, 6, 8};

    private final GameEngineClient gameEngineClient;
    private final SessionRepository sessionRepository;
    private final SimulationRepository simulationRepository;
    private final SseEmitterRegistry emitterRegistry;

    public SessionServiceImpl(GameEngineClient gameEngineClient,
                               SessionRepository sessionRepository,
                               SimulationRepository simulationRepository,
                               SseEmitterRegistry emitterRegistry) {
        this.gameEngineClient = gameEngineClient;
        this.sessionRepository = sessionRepository;
        this.simulationRepository = simulationRepository;
        this.emitterRegistry = emitterRegistry;
    }

    @Override
    public GameSession createSession() {
        GameSession session = new GameSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setGameId(UUID.randomUUID().toString());
        session.setStatus(SessionStatus.CREATED);
        session.setMoveHistory(new ArrayList<>());
        return sessionRepository.save(session);
    }

    @Override
    public GameSession simulate(String sessionId) {
        GameSession session = getSession(sessionId);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setMoveHistory(new ArrayList<>());

        Simulation simulation = new Simulation();
        simulation.setId(UUID.randomUUID().toString());
        simulation.setSessionId(sessionId);
        simulation.setErrorsCount(0);
        simulation.setStartedAt(Instant.now());
        simulation.setStatus(SessionStatus.IN_PROGRESS);
        simulationRepository.save(simulation);

        Thread.ofVirtual().start(() -> runMockSimulation(session, simulation));

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

    @Override
    public GameSession cancel(String sessionId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void runMockSimulation(GameSession session, Simulation simulation) {
        boolean useWinScript = ThreadLocalRandom.current().nextBoolean();
        String[] players = useWinScript ? WIN_SCRIPT_PLAYERS : DRAW_SCRIPT_PLAYERS;
        int[] positions = useWinScript ? WIN_SCRIPT_POSITIONS : DRAW_SCRIPT_POSITIONS;
        String winner = useWinScript ? WIN_SCRIPT_WINNER : null;

        List<String> board = new ArrayList<>(Collections.nCopies(BOARD_SIZE, null));

        for (int i = 0; i < players.length; i++) {
            sleep(MOVE_DELAY_MS);

            String player = players[i];
            int position = positions[i];
            board.set(position, player);

            MoveRecord move = new MoveRecord();
            move.setPlayer(player);
            move.setPosition(position);
            session.getMoveHistory().add(move);

            boolean isLastMove = i == players.length - 1;
            SessionStatus status = isLastMove
                    ? (winner != null ? SessionStatus.X_WON : SessionStatus.DRAW)
                    : SessionStatus.IN_PROGRESS;
            session.setStatus(status);

            emitterRegistry.publish(session.getSessionId(), new SessionEvent(
                    session.getSessionId(), new ArrayList<>(board), status, isLastMove ? winner : null));
        }

        emitterRegistry.complete(session.getSessionId());

        simulation.setStatus(session.getStatus());
        simulation.setFinishedAt(Instant.now());
        simulationRepository.save(simulation);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}