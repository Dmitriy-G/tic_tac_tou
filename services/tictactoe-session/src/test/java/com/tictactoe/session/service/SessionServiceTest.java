package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SessionMoveEntity;
import com.tictactoe.session.entity.SessionMoveId;
import com.tictactoe.session.exception.NotSessionOwnerException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SessionMoveJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * getSession must be able to rebuild the full session view on its own (E9) — status, board,
 * moves, winner — since SSE is only an optimisation on top of it, not the only way a client can
 * observe a session. There is no in-memory store involved anywhere in this file anymore: every
 * fixture is a database row, per {@code docs/adr/0004-session-is-the-game.md}.
 */
class SessionServiceTest {

    private final SessionJpaRepository sessionRepository = mock(SessionJpaRepository.class);
    private final SessionMoveJpaRepository moveRepository = mock(SessionMoveJpaRepository.class);
    private final SseEmitterRegistry emitterRegistry = mock(SseEmitterRegistry.class);
    private final SimulationStarter simulationStarter = mock(SimulationStarter.class);
    private final OwnerTokenService ownerTokenService = new OwnerTokenService();
    private final SessionService sessionService =
            new SessionService(sessionRepository, moveRepository, emitterRegistry, simulationStarter, ownerTokenService);

    private final UUID sessionUuid = UUID.randomUUID();
    private final String sessionId = sessionUuid.toString();

    @Test
    void simulateWithoutOwnerTokenThrowsNotSessionOwner() {
        SessionEntity entity = freshEntity();
        entity.setOwnerTokenHash(ownerTokenService.hash("correct-token"));
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> sessionService.simulate(sessionId, null))
                .isInstanceOf(NotSessionOwnerException.class);
    }

    @Test
    void simulateWithWrongOwnerTokenThrowsNotSessionOwner() {
        SessionEntity entity = freshEntity();
        entity.setOwnerTokenHash(ownerTokenService.hash("correct-token"));
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> sessionService.simulate(sessionId, "wrong-token"))
                .isInstanceOf(NotSessionOwnerException.class);
    }

    @Test
    void simulateOnUnknownSessionThrowsSessionNotFoundNotForbidden() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.simulate(sessionId, null))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void simulateWithCorrectOwnerTokenStartsTheSimulation() {
        SessionEntity entity = freshEntity();
        entity.setOwnerTokenHash(ownerTokenService.hash("correct-token"));
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));

        sessionService.simulate(sessionId, "correct-token");

        verify(simulationStarter).start(sessionId);
    }

    @Test
    void createSessionReturnsAFreshOwnerTokenAndPersistsOnlyItsHash() {
        SessionResponse response = sessionService.createSession();

        assertThat(response.ownerToken()).isNotBlank();
        org.mockito.ArgumentCaptor<SessionEntity> captor = org.mockito.ArgumentCaptor.forClass(SessionEntity.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerTokenHash())
                .isEqualTo(ownerTokenService.hash(response.ownerToken()))
                .isNotEqualTo(response.ownerToken());
    }

    @Test
    void createSessionReturnsCreatedStatusWithAnEmptyBoardAndNoMoves() {
        SessionResponse response = sessionService.createSession();

        assertThat(response.status()).isEqualTo(GameState.CREATED);
        assertThat(response.board()).containsOnly(".");
        assertThat(response.moves()).isEmpty();
        assertThat(response.winner()).isNull();
    }

    @Test
    void getSessionDoesNotExposeAnOwnerToken() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(freshEntity()));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.ownerToken()).isNull();
    }

    @Test
    void getSessionOnUnknownIdThrowsSessionNotFound() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSession(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void getSessionOnAFreshSessionReturnsCreatedWithAnEmptyBoard() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(freshEntity()));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.CREATED);
        assertThat(response.board()).containsOnly(".");
        assertThat(response.moves()).isEmpty();
        assertThat(response.winner()).isNull();
    }

    @Test
    void getSessionAfterAMoveReflectsTheLatestBoardAndMoveHistory() {
        SessionEntity entity = freshEntity();
        entity.setStatus(GameState.IN_PROGRESS);
        entity.setBoard("X........");
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));
        when(moveRepository.findBySessionIdOrderByMoveNumber(sessionUuid))
                .thenReturn(List.of(move(sessionUuid, 1, Symbol.X, 0)));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(response.board()).containsExactly("X", ".", ".", ".", ".", ".", ".", ".", ".");
        assertThat(response.moves()).hasSize(1);
        assertThat(response.moves().get(0).symbol()).isEqualTo(Symbol.X);
        assertThat(response.moves().get(0).position()).isEqualTo(0);
    }

    @Test
    void getSessionAfterAFailureReportsTheErrorCodeAndMessage() {
        SessionEntity entity = freshEntity();
        entity.setStatus(GameState.FAILED);
        entity.setErrorCode("ENGINE_UNAVAILABLE");
        entity.setErrorMessage("boom");
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.FAILED);
        assertThat(response.errorCode()).isEqualTo("ENGINE_UNAVAILABLE");
        assertThat(response.errorMessage()).isEqualTo("boom");
    }

    @Test
    void getSessionAfterARestartStillReportsTheTerminalState() {
        // The point of the whole refactor: nothing here is a SessionStateStore fixture. This
        // entity and its moves are exactly what a *different* process would have written to
        // session_db before this one even started, and getSession rebuilds the full terminal
        // view from them alone.
        SessionEntity entity = freshEntity();
        entity.setStatus(GameState.X_WON);
        entity.setBoard("XXX......");
        entity.setWinner(Symbol.X);
        entity.setErrorsCount(0);
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));
        when(moveRepository.findBySessionIdOrderByMoveNumber(sessionUuid)).thenReturn(List.of(
                move(sessionUuid, 1, Symbol.X, 0), move(sessionUuid, 2, Symbol.O, 3), move(sessionUuid, 3, Symbol.X, 1),
                move(sessionUuid, 4, Symbol.O, 4), move(sessionUuid, 5, Symbol.X, 2)));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.X_WON);
        assertThat(response.board()).containsExactly("X", "X", "X", ".", ".", ".", ".", ".", ".");
        assertThat(response.winner()).isEqualTo(Symbol.X);
        assertThat(response.moves()).hasSize(5);
        assertThat(response.moves().get(4).moveNumber()).isEqualTo(5);
    }

    private static SessionMoveEntity move(UUID sessionId, int moveNumber, Symbol symbol, int position) {
        SessionMoveEntity move = new SessionMoveEntity();
        move.setId(new SessionMoveId(sessionId, (short) moveNumber));
        move.setSymbol(symbol);
        move.setPosition((short) position);
        move.setStepStatus(StepStatus.CORRECT_STEP);
        move.setCreatedAt(Instant.now());
        return move;
    }

    private SessionEntity freshEntity() {
        SessionEntity entity = new SessionEntity();
        entity.setId(sessionUuid);
        entity.setOwnerTokenHash("hash");
        entity.setStatus(GameState.CREATED);
        entity.setBoard(".........");
        entity.setErrorsCount(0);
        return entity;
    }
}