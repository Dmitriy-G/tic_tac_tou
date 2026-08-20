package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.MoveStrategyResolver;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SessionMoveEntity;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SessionMoveJpaRepository;
import com.tictactoe.session.sse.SseEmitterRegistry;
import com.tictactoe.session.strategy.MoveStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterisation tests for the simulation loop, exercised end-to-end through
 * {@link SimulationRunner} and the real {@link SimulationStep}, {@link SessionStateWriter} and
 * {@link SimulationEventPublisher} it wires together. Only the true boundaries are mocked:
 * {@link GameEngineClient}, {@link SessionJpaRepository}, {@link SessionMoveJpaRepository} and
 * {@link SseEmitterRegistry}. The move strategy is a fixed, deterministic stub — no randomness.
 *
 * <p>{@code sessionId} now doubles as the engine's {@code gameId} (one session is one game, see
 * {@code docs/adr/0004-session-is-the-game.md}), so the old separate {@code simulationId} is
 * gone — a single UUID drives engine calls, the persisted row lookup, and the SSE registry key.
 */
class SimulationRunnerTest {

    private static final List<String> INITIAL_BOARD = List.of("_", "_", "_", "_", "_", "_", "_", "_", "_");

    private final UUID sessionUuid = UUID.randomUUID();
    private final String sessionId = sessionUuid.toString();

    private GameEngineClient gameEngineClient;
    private SessionJpaRepository sessionRepository;
    private SessionMoveJpaRepository moveRepository;
    private SseEmitterRegistry emitterRegistry;
    private SessionEntity entity;
    private SimulationRunner runner;

    @BeforeEach
    void setUp() {
        gameEngineClient = mock(GameEngineClient.class);
        sessionRepository = mock(SessionJpaRepository.class);
        moveRepository = mock(SessionMoveJpaRepository.class);
        emitterRegistry = mock(SseEmitterRegistry.class);

        entity = new SessionEntity();
        entity.setId(sessionUuid);
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entity));

        when(gameEngineClient.createGame(sessionId)).thenReturn(new CreateGameResponse(INITIAL_BOARD));

        MoveStrategyResolver moveStrategyResolver = mock(MoveStrategyResolver.class);
        AtomicInteger nextPosition = new AtomicInteger(0);
        MoveStrategy fixedStrategy = (board, symbol) -> nextPosition.getAndIncrement();
        when(moveStrategyResolver.resolve(anyString())).thenReturn(fixedStrategy);

        SimulationEventPublisher eventPublisher = new SimulationEventPublisher(emitterRegistry);
        SessionStateWriter stateWriter = new SessionStateWriter(sessionRepository, moveRepository);
        SimulationStep step = new SimulationStep(gameEngineClient, moveStrategyResolver, eventPublisher);
        runner = new SimulationRunner(gameEngineClient, step, stateWriter, eventPublisher);
    }

    @Test
    void everyMoveIsForwardedToTheEngine() {
        stubNineMovesEndingInDraw();

        runner.run(sessionId, "trace-1");

        verify(gameEngineClient, times(9)).move(eq(sessionId), anyString(), anyInt());
    }

    @Test
    void everyMovePublishesAnSseEvent() {
        stubNineMovesEndingInDraw();

        runner.run(sessionId, "trace-1");

        verify(emitterRegistry, times(9)).publish(eq(sessionId), any());
    }

    @Test
    void everyMoveIsPersisted() {
        stubNineMovesEndingInDraw();

        runner.run(sessionId, "trace-1");

        verify(sessionRepository, times(9)).save(entity);
        verify(moveRepository, times(9)).save(any(SessionMoveEntity.class));
    }

    @Test
    void aDrawIsRecordedAsTheTerminalStatus() {
        stubNineMovesEndingInDraw();

        runner.run(sessionId, "trace-1");

        assertThat(entity.getStatus()).isEqualTo(GameState.DRAW);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void theEmitterIsCompletedExactlyOnce() {
        stubNineMovesEndingInDraw();

        runner.run(sessionId, "trace-1");

        verify(emitterRegistry, times(1)).complete(sessionId);
    }

    private void stubNineMovesEndingInDraw() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.DRAW));
    }

    @Test
    void terminalStateAtMoveFiveStopsTheLoopImmediately() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.X_WON));

        runner.run(sessionId, "trace-1");

        verify(gameEngineClient, times(5)).move(eq(sessionId), anyString(), anyInt());
        verify(sessionRepository, times(5)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.X_WON);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void everyStepInvalidFailsAfterElevenIterations() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt())).thenReturn(invalidStep());

        runner.run(sessionId, "trace-1");

        verify(gameEngineClient, times(11)).move(eq(sessionId), anyString(), anyInt());
        verify(sessionRepository, times(11)).save(entity);
        // None of the 11 attempts was CORRECT_STEP, so moveNumber never advanced past 1 and no
        // session_moves row was ever written — persisting every rejected attempt at the same
        // move number would collide with the composite primary key by design.
        verify(moveRepository, times(0)).save(any(SessionMoveEntity.class));
        assertThat(entity.getErrorsCount()).isEqualTo(11);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void theLoopStopsAfterTwelveIterations() {
        stubNineSuccessesAndThreeErrors();

        runner.run(sessionId, "trace-1");

        verify(gameEngineClient, times(12)).move(eq(sessionId), anyString(), anyInt());
    }

    @Test
    void theSessionIsClosedOutAsFailed() {
        stubNineSuccessesAndThreeErrors();

        runner.run(sessionId, "trace-1");

        assertThat(entity.getErrorsCount()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        assertThat(entity.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
    }

    private void stubNineSuccessesAndThreeErrors() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS));
    }

    @Test
    void engineExceptionMidRunIsPersistedNotifiedThenEmitterIsCompleted() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt()))
                .thenReturn(correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS))
                .thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(sessionId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(3)).move(eq(sessionId), anyString(), anyInt());
        // 2 in-loop recordMove() calls plus the fail() call from the catch block.
        verify(sessionRepository, times(3)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        assertThat(entity.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(entity.getErrorMessage()).isEqualTo("engine unreachable");

        ArgumentCaptor<SessionEvent> eventCaptor = ArgumentCaptor.forClass(SessionEvent.class);
        verify(emitterRegistry, times(3)).publish(eq(sessionId), eventCaptor.capture());
        SessionEvent failureEvent = eventCaptor.getAllValues().get(2);
        assertThat(failureEvent.type()).isEqualTo(SessionEvent.EventType.FAILED);
        assertThat(failureEvent.errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(failureEvent.errorMessage()).isEqualTo("engine unreachable");

        verify(emitterRegistry, times(1)).complete(sessionId);
    }

    @Test
    void engineExceptionAtGameCreationStillLeavesTheRowInATerminalState() {
        when(gameEngineClient.createGame(sessionId)).thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(sessionId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(0)).move(eq(sessionId), anyString(), anyInt());
        verify(sessionRepository, times(1)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        verify(emitterRegistry, times(1)).publish(eq(sessionId), any());
        verify(emitterRegistry, times(1)).complete(sessionId);
    }

    @Test
    void engineExceptionOnTheVeryFirstMoveStillLeavesTheRowInATerminalState() {
        when(gameEngineClient.move(eq(sessionId), anyString(), anyInt()))
                .thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(sessionId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(1)).move(eq(sessionId), anyString(), anyInt());
        verify(sessionRepository, times(1)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        verify(emitterRegistry, times(1)).complete(sessionId);
    }

    private static MoveResponse correctStep(GameState gameState) {
        return new MoveResponse(INITIAL_BOARD, gameState, StepStatus.CORRECT_STEP, null);
    }

    private static MoveResponse invalidStep() {
        return new MoveResponse(INITIAL_BOARD, GameState.IN_PROGRESS, StepStatus.CELL_OCCUPIED, null);
    }
}