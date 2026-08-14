package com.tictactoe.session.repository;

import com.tictactoe.session.SessionApplication;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SimulationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SessionApplication.class)
@ActiveProfiles("test")
class PostgresSessionRepositoryTest {

    @Autowired
    private SessionJpaRepository sessionRepository;

    @Autowired
    private SimulationJpaRepository simulationRepository;

    @Test
    void savesAndReloadsASessionIdentity() {
        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash("test-owner-token-hash");

        sessionRepository.save(session);
        Optional<SessionEntity> reloaded = sessionRepository.findById(sessionId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(sessionId);
    }

    @Test
    void returnsEmptyForUnknownSessionId() {
        assertThat(sessionRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findsSimulationsForASessionOrderedByStartedAtDesc() {
        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash("test-owner-token-hash");
        sessionRepository.save(session);

        SimulationEntity simulation = new SimulationEntity();
        simulation.setId(UUID.randomUUID());
        simulation.setSessionId(sessionId);
        simulation.setErrorsCount(0);
        simulation.setStartedAt(Instant.now());
        simulation.setFinishedAt(Instant.now());
        simulation.setStatus(GameState.X_WON);
        simulationRepository.save(simulation);

        List<SimulationEntity> found = simulationRepository.findBySessionIdOrderByStartedAtDesc(sessionId);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getSessionId()).isEqualTo(sessionId);
        assertThat(found.getFirst().getStatus()).isEqualTo(GameState.X_WON);
    }
}