package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.client.GameEngineClient;
import com.tictactoe.session.config.MoveStrategyResolver;
import com.tictactoe.session.domain.SessionEvent;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.repository.SimulationJpaRepository;
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
 * {@link SimulationRunner} and the real {@link SimulationStep}, {@link SimulationStateWriter}
 * and {@link SimulationEventPublisher} it wires together. Only the true boundaries are mocked:
 * {@link GameEngineClient}, {@link SimulationJpaRepository} and {@link SseEmitterRegistry}. The
 * move strategy is a fixed, deterministic stub — no randomness.
 */
class SimulationRunnerTest {

    private static final List<String> INITIAL_BOARD = List.of("_", "_", "_", "_", "_", "_", "_", "_", "_");
    private static final String SESSION_ID = "session-1";

    private final UUID simulationUuid = UUID.randomUUID();
    private final String simulationId = simulationUuid.toString();

    private GameEngineClient gameEngineClient;
    private SimulationJpaRepository simulationRepository;
    private SseEmitterRegistry emitterRegistry;
    private SimulationEntity entity;
    private SimulationRunner runner;

    @BeforeEach
    void setUp() {
        gameEngineClient = mock(GameEngineClient.class);
        simulationRepository = mock(SimulationJpaRepository.class);
        emitterRegistry = mock(SseEmitterRegistry.class);

        entity = new SimulationEntity();
        entity.setId(simulationUuid);
        when(simulationRepository.findById(simulationUuid)).thenReturn(Optional.of(entity));

        when(gameEngineClient.createGame(simulationId)).thenReturn(new CreateGameResponse(INITIAL_BOARD));

        MoveStrategyResolver moveStrategyResolver = mock(MoveStrategyResolver.class);
        AtomicInteger nextPosition = new AtomicInteger(0);
        MoveStrategy fixedStrategy = (board, symbol) -> nextPosition.getAndIncrement();
        when(moveStrategyResolver.resolve(anyString())).thenReturn(fixedStrategy);

        SimulationEventPublisher eventPublisher = new SimulationEventPublisher(emitterRegistry, new SessionStateStore());
        SimulationStateWriter stateWriter = new SimulationStateWriter(simulationRepository);
        SimulationStep step = new SimulationStep(gameEngineClient, moveStrategyResolver, eventPublisher);
        runner = new SimulationRunner(gameEngineClient, step, stateWriter, eventPublisher);
    }

    @Test
    void allNineMovesCorrectEndsInDrawWithFinishedAtSet() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.DRAW));

        runner.run(SESSION_ID, simulationId, "trace-1");

        verify(gameEngineClient, times(9)).move(eq(simulationId), anyString(), anyInt());
        verify(emitterRegistry, times(9)).publish(eq(SESSION_ID), any());
        verify(simulationRepository, times(9)).save(entity);
        verify(emitterRegistry, times(1)).complete(SESSION_ID);
        assertThat(entity.getStatus()).isEqualTo(GameState.DRAW);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void terminalStateAtMoveFiveStopsTheLoopImmediately() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS),
                correctStep(GameState.X_WON));

        runner.run(SESSION_ID, simulationId, "trace-1");

        verify(gameEngineClient, times(5)).move(eq(simulationId), anyString(), anyInt());
        verify(simulationRepository, times(5)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.X_WON);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void everyStepInvalidFailsAfterElevenIterations() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt())).thenReturn(invalidStep());

        runner.run(SESSION_ID, simulationId, "trace-1");

        verify(gameEngineClient, times(11)).move(eq(simulationId), anyString(), anyInt());
        verify(simulationRepository, times(11)).save(entity);
        assertThat(entity.getErrorsCount()).isEqualTo(11);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
    }

    @Test
    void nineValidMovesWithoutATerminalStateFromEngineIsClosedOutAsFailed() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt())).thenReturn(
                correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), invalidStep(),
                correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS));

        runner.run(SESSION_ID, simulationId, "trace-1");

        verify(gameEngineClient, times(12)).move(eq(simulationId), anyString(), anyInt());
        // 12 in-loop persist() calls (the loop exhausts its move budget at move 12) plus one
        // more from persistTerminal(), which closes the IN_PROGRESS-forever hole (E5).
        verify(simulationRepository, times(13)).save(entity);
        assertThat(entity.getErrorsCount()).isEqualTo(3);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        assertThat(entity.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(entity.getRunningSessionId()).isNull();
    }

    @Test
    void engineExceptionMidRunIsPersistedNotifiedThenEmitterIsCompleted() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt()))
                .thenReturn(correctStep(GameState.IN_PROGRESS), correctStep(GameState.IN_PROGRESS))
                .thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(SESSION_ID, simulationId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(3)).move(eq(simulationId), anyString(), anyInt());
        // 2 in-loop persist() calls plus the fail() call from the catch block.
        verify(simulationRepository, times(3)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        assertThat(entity.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(entity.getErrorMessage()).isEqualTo("engine unreachable");

        ArgumentCaptor<SessionEvent> eventCaptor = ArgumentCaptor.forClass(SessionEvent.class);
        verify(emitterRegistry, times(3)).publish(eq(SESSION_ID), eventCaptor.capture());
        SessionEvent failureEvent = eventCaptor.getAllValues().get(2);
        assertThat(failureEvent.type()).isEqualTo(SessionEvent.EventType.FAILED);
        assertThat(failureEvent.errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(failureEvent.errorMessage()).isEqualTo("engine unreachable");

        verify(emitterRegistry, times(1)).complete(SESSION_ID);
    }

    @Test
    void engineExceptionAtGameCreationStillLeavesTheRowInATerminalState() {
        when(gameEngineClient.createGame(simulationId)).thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(SESSION_ID, simulationId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(0)).move(eq(simulationId), anyString(), anyInt());
        verify(simulationRepository, times(1)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        verify(emitterRegistry, times(1)).publish(eq(SESSION_ID), any());
        verify(emitterRegistry, times(1)).complete(SESSION_ID);
    }

    @Test
    void engineExceptionOnTheVeryFirstMoveStillLeavesTheRowInATerminalState() {
        when(gameEngineClient.move(eq(simulationId), anyString(), anyInt()))
                .thenThrow(new RuntimeException("engine unreachable"));

        assertThatCode(() -> runner.run(SESSION_ID, simulationId, "trace-1")).doesNotThrowAnyException();

        verify(gameEngineClient, times(1)).move(eq(simulationId), anyString(), anyInt());
        verify(simulationRepository, times(1)).save(entity);
        assertThat(entity.getStatus()).isEqualTo(GameState.FAILED);
        assertThat(entity.getFinishedAt()).isNotNull();
        verify(emitterRegistry, times(1)).complete(SESSION_ID);
    }

    private static MoveResponse correctStep(GameState gameState) {
        return new MoveResponse(INITIAL_BOARD, gameState, StepStatus.CORRECT_STEP, null);
    }

    private static MoveResponse invalidStep() {
        return new MoveResponse(INITIAL_BOARD, GameState.IN_PROGRESS, StepStatus.CELL_OCCUPIED, null);
    }
}