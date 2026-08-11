package com.tictactoe.gamesession.service;

import com.tictactoe.gamesession.client.GameEngineClient;
import com.tictactoe.gamesession.exception.SessionNotFoundException;
import com.tictactoe.gamesession.model.GameSession;
import com.tictactoe.gamesession.model.MoveRecord;
import com.tictactoe.gamesession.model.SessionEvent;
import com.tictactoe.gamesession.model.SessionStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SessionServiceImpl(GameEngineClient gameEngineClient) {
        this.gameEngineClient = gameEngineClient;
    }

    @Override
    public GameSession createSession() {
        GameSession session = new GameSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setGameId(UUID.randomUUID().toString());
        session.setStatus(SessionStatus.CREATED);
        session.setMoveHistory(new ArrayList<>());
        sessions.put(session.getSessionId(), session);
        return session;
    }

    @Override
    public GameSession simulate(String sessionId) {
        GameSession session = getSession(sessionId);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setMoveHistory(new ArrayList<>());

        Thread.ofVirtual().start(() -> runMockSimulation(session));

        return session;
    }

    @Override
    public GameSession getSession(String sessionId) {
        GameSession session = sessions.get(sessionId);
        if (session == null) {
            throw new SessionNotFoundException(sessionId);
        }
        return session;
    }

    @Override
    public SseEmitter subscribe(String sessionId) {
        getSession(sessionId);

        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId, emitter));
        emitter.onTimeout(() -> emitters.remove(sessionId, emitter));
        emitter.onError(throwable -> emitters.remove(sessionId, emitter));
        return emitter;
    }

    private void runMockSimulation(GameSession session) {
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
                    ? (winner != null ? SessionStatus.WIN : SessionStatus.DRAW)
                    : SessionStatus.IN_PROGRESS;
            session.setStatus(status);

            publish(session.getSessionId(), new SessionEvent(
                    session.getSessionId(), new ArrayList<>(board), status, isLastMove ? winner : null));
        }

        completeStream(session.getSessionId());
    }

    private void publish(String sessionId, SessionEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("update").data(event, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(sessionId, emitter);
        }
    }

    private void completeStream(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}