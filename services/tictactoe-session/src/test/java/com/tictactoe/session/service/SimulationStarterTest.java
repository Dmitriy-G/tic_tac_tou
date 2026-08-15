package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SimulationEntity;
import com.tictactoe.session.exception.SessionAlreadyCompletedException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.repository.SessionJpaRepository;
import com.tictactoe.session.repository.SimulationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationStarterTest {

    private final UUID sessionUuid = UUID.randomUUID();
    private final String sessionId = sessionUuid.toString();

    private SessionJpaRepository sessionRepository;
    private SimulationJpaRepository simulationRepository;
    private SessionStateStore stateStore;
    private SimulationStarter starter;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionJpaRepository.class);
        simulationRepository = mock(SimulationJpaRepository.class);
        SimulationRunner runner = mock(SimulationRunner.class);
        stateStore = mock(SessionStateStore.class);
        starter = new SimulationStarter(sessionRepository, simulationRepository, runner, stateStore);

        when(sessionRepository.existsById(sessionUuid)).thenReturn(true);
        when(simulationRepository.existsBySessionIdAndStatus(eq(sessionUuid), eq(GameState.IN_PROGRESS)))
                .thenReturn(false);
    }

    @Test
    void rejectsReSimulationOfASessionWithATerminalSimulation() {
        SimulationEntity finished = new SimulationEntity();
        finished.setStatus(GameState.DRAW);
        when(simulationRepository.findBySessionIdOrderByStartedAtDesc(sessionUuid)).thenReturn(List.of(finished));

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SessionAlreadyCompletedException.class);

        verify(simulationRepository, never()).save(any());
        verify(stateStore, never()).markRunning(sessionId);
    }

    @Test
    void allowsTheFirstSimulationForASessionWithNoPriorRuns() {
        when(simulationRepository.findBySessionIdOrderByStartedAtDesc(sessionUuid)).thenReturn(List.of());

        assertThatCode(() -> starter.start(sessionId)).doesNotThrowAnyException();
    }

    @Test
    void unknownSessionIsRejectedBeforeTheCompletedCheck() {
        when(sessionRepository.existsById(sessionUuid)).thenReturn(false);

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SessionNotFoundException.class);

        verify(simulationRepository, never()).findBySessionIdOrderByStartedAtDesc(any());
    }

    @Test
    void aRunningSimulationIsRejectedBeforeTheCompletedCheck() {
        when(simulationRepository.existsBySessionIdAndStatus(eq(sessionUuid), eq(GameState.IN_PROGRESS)))
                .thenReturn(true);

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SimulationAlreadyRunningException.class);

        verify(simulationRepository, never()).findBySessionIdOrderByStartedAtDesc(any());
    }
}
