package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.exception.SessionAlreadyCompletedException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationAlreadyRunningException;
import com.tictactoe.session.repository.SessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The double-simulate guard is now a single compare-and-swap ({@link SessionJpaRepository#claimForSimulation})
 * instead of a read-check plus an insert relying on a unique index — see
 * {@code docs/adr/0004-session-is-the-game.md}. These tests assert the CAS contract directly:
 * {@code claimForSimulation} returning {@code 1} means this call won and a simulation starts;
 * {@code 0} means it lost, and the pre-claim read of the session's status (never re-read after
 * the CAS) decides which 409 the caller sees.
 */
class SimulationStarterTest {

    private final UUID sessionUuid = UUID.randomUUID();
    private final String sessionId = sessionUuid.toString();

    private SessionJpaRepository sessionRepository;
    private SimulationRunner runner;
    private SimulationStarter starter;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionJpaRepository.class);
        runner = mock(SimulationRunner.class);
        starter = new SimulationStarter(sessionRepository, runner);
    }

    @Test
    void unknownSessionIsRejectedBeforeTheClaimAttempt() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SessionNotFoundException.class);

        verify(sessionRepository, never()).claimForSimulation(any(), any());
    }

    @Test
    void aWonClaimStartsTheSimulation() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entityWithStatus(GameState.CREATED)));
        when(sessionRepository.claimForSimulation(eq(sessionUuid), any(Instant.class))).thenReturn(1);

        assertThatCode(() -> starter.start(sessionId)).doesNotThrowAnyException();
    }

    @Test
    void aLostClaimAgainstARunningSimulationThrowsSimulationAlreadyRunning() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entityWithStatus(GameState.IN_PROGRESS)));
        when(sessionRepository.claimForSimulation(eq(sessionUuid), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SimulationAlreadyRunningException.class);
    }

    @Test
    void aLostClaimAgainstATerminalSessionThrowsSessionAlreadyCompleted() {
        when(sessionRepository.findById(sessionUuid)).thenReturn(Optional.of(entityWithStatus(GameState.DRAW)));
        when(sessionRepository.claimForSimulation(eq(sessionUuid), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> starter.start(sessionId)).isInstanceOf(SessionAlreadyCompletedException.class);
    }

    private static SessionEntity entityWithStatus(GameState status) {
        SessionEntity entity = new SessionEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus(status);
        return entity;
    }
}