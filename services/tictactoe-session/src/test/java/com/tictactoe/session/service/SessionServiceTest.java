package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.session.dto.SessionResponse;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * getSession must be able to rebuild the full session view on its own (E9) — status, board,
 * moves, winner — since SSE is only an optimisation on top of it, not the only way a client can
 * observe a session.
 */
class SessionServiceTest {

    private final SessionJpaRepository sessionRepository = mock(SessionJpaRepository.class);
    private final SseEmitterRegistry emitterRegistry = mock(SseEmitterRegistry.class);
    private final SimulationStarter simulationStarter = mock(SimulationStarter.class);
    private final SessionStateStore stateStore = new SessionStateStore();
    private final SessionService sessionService =
            new SessionService(sessionRepository, emitterRegistry, simulationStarter, stateStore);

    private final UUID sessionUuid = UUID.randomUUID();
    private final String sessionId = sessionUuid.toString();

    @BeforeEach
    void setUp() {
        when(sessionRepository.existsById(sessionUuid)).thenReturn(true);
    }

    @Test
    void getSessionOnUnknownIdThrowsSessionNotFound() {
        when(sessionRepository.existsById(sessionUuid)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.getSession(sessionId))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void getSessionOnAFreshSessionReturnsCreatedWithAnEmptyBoard() {
        stateStore.initialize(sessionId);

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.CREATED);
        assertThat(response.board()).containsOnly(".");
        assertThat(response.moves()).isEmpty();
        assertThat(response.winner()).isNull();
    }

    @Test
    void getSessionAfterAMoveReflectsTheLatestBoardAndMoveHistory() {
        stateStore.initialize(sessionId);
        stateStore.markRunning(sessionId);
        List<String> board = List.of("X", ".", ".", ".", ".", ".", ".", ".", ".");
        stateStore.recordMove(sessionId, 1, Symbol.X, 0,
                new MoveResponse(board, GameState.IN_PROGRESS, StepStatus.CORRECT_STEP, null));

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(response.board()).isEqualTo(board);
        assertThat(response.moves()).hasSize(1);
        assertThat(response.moves().get(0).symbol()).isEqualTo(Symbol.X);
        assertThat(response.moves().get(0).position()).isEqualTo(0);
    }

    @Test
    void getSessionAfterAFailureReportsTheErrorCodeAndMessage() {
        stateStore.initialize(sessionId);
        stateStore.markRunning(sessionId);
        stateStore.recordFailure(sessionId, com.tictactoe.common.error.ErrorCode.ENGINE_UNAVAILABLE, "boom");

        SessionResponse response = sessionService.getSession(sessionId);

        assertThat(response.status()).isEqualTo(GameState.FAILED);
        assertThat(response.errorCode()).isEqualTo("ENGINE_UNAVAILABLE");
        assertThat(response.errorMessage()).isEqualTo("boom");
    }
}
