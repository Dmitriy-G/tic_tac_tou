package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.common.error.InternalException;
import com.tictactoe.session.dto.MoveRecord;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SessionMoveEntity;
import com.tictactoe.session.entity.SessionMoveId;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SessionMoveJpaRepository;
import com.tictactoe.session.util.BoardUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Persists simulation progress and terminal outcomes to {@code session_db.sessions} and
 * {@code session_db.session_moves} — replaces the deleted {@code SessionStateStore} (in-memory)
 * and {@code SimulationStateWriter} ({@code simulations} table) now that one session is one game
 * and the session row is the single source of truth. See
 * {@code docs/adr/0004-session-is-the-game.md}.
 */
@Component
public class SessionStateWriter {

    private static final Logger log = LoggerFactory.getLogger(SessionStateWriter.class);

    private final SessionJpaRepository sessionRepository;
    private final SessionMoveJpaRepository moveRepository;

    public SessionStateWriter(SessionJpaRepository sessionRepository, SessionMoveJpaRepository moveRepository) {
        this.sessionRepository = sessionRepository;
        this.moveRepository = moveRepository;
    }

    /**
     * Writes one {@code INSERT} into {@code session_moves} (only when {@code move} actually
     * landed — a rejected attempt never advances {@code moveNumber}, so it would collide with the
     * composite primary key) plus one {@code UPDATE} on {@code sessions} with the latest board,
     * status, winner and error count. Known trade-off: this happens on every move, which is fine
     * for a nine-move game and is what makes the state durable, but would not scale to mass
     * simulation — see the ADR's Consequences section.
     */
    void recordMove(String sessionId, SimulationProgress progress, MoveRecord move) {
        UUID id = UUID.fromString(sessionId);
        if (StepStatus.CORRECT_STEP.equals(move.stepStatus())) {
            SessionMoveEntity moveEntity = new SessionMoveEntity();
            moveEntity.setId(new SessionMoveId(id, (short) move.moveNumber()));
            moveEntity.setSymbol(move.symbol());
            moveEntity.setPosition((short) move.position());
            moveEntity.setStepStatus(move.stepStatus());
            moveEntity.setCreatedAt(Instant.now());
            moveRepository.save(moveEntity);
        }

        SessionEntity session = findOrThrow(sessionId);
        boolean stillRunning = GameState.IN_PROGRESS.equals(progress.gameState());
        session.setBoard(BoardUtils.convertToString(progress.board()));
        session.setStatus(progress.gameState());
        session.setErrorsCount(progress.errorsCount());
        session.setWinner(winnerOf(progress.gameState()));
        session.setFinishedAt(stillRunning ? null : Instant.now());
        sessionRepository.save(session);
    }

    /**
     * Closes the loop-exit hole: if {@link SimulationRunner}'s loop stopped because it exhausted
     * its move budget without the engine ever returning a terminal state, this forces a
     * {@code FAILED} row via {@link #fail}. A no-op when {@code progress} is already terminal,
     * which is the normal case since the last {@link #recordMove} call already wrote it.
     */
    void persistTerminal(String sessionId, SimulationProgress progress) {
        if (GameState.IN_PROGRESS.equals(progress.gameState())) {
            fail(sessionId, ErrorCode.INTERNAL_ERROR,
                    "Simulation exhausted its move budget without the engine reaching a terminal state");
        }
    }

    /**
     * Persists a terminal {@code FAILED} outcome with the given error code/message. Swallows its
     * own {@code RuntimeException} (logs and returns) so a failure to record a failure can never
     * mask the original exception {@link SimulationRunner} is already handling.
     */
    void fail(String sessionId, ErrorCode errorCode, String message) {
        try {
            SessionEntity session = findOrThrow(sessionId);
            session.setStatus(GameState.FAILED);
            session.setFinishedAt(Instant.now());
            session.setErrorCode(errorCode.getCode());
            session.setErrorMessage(message);
            sessionRepository.save(session);
        } catch (RuntimeException e) {
            log.error("failed to persist FAILED state for session {}", sessionId, e);
        }
    }

    private SessionEntity findOrThrow(String sessionId) {
        return sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR,
                        "Session " + sessionId + " vanished mid-run"));
    }

    private static Symbol winnerOf(GameState state) {
        return switch (state) {
            case X_WON -> Symbol.X;
            case O_WON -> Symbol.O;
            default -> null;
        };
    }
}