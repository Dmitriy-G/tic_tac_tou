package com.tictactoe.session.repository;

import com.tictactoe.session.SessionApplication;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SimulationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Runs on H2 in PostgreSQL compatibility mode ({@code jdbc:h2:mem:session_db;MODE=PostgreSQL} in
 * application-test.yml), not real Postgres — named accordingly. save()/findById() themselves are
 * Spring Data JPA framework behaviour and are not re-tested here; this class covers this service's
 * own query methods and the database-level constraints in the Flyway migrations.
 *
 * <p>The constraint tests below assume the migrations stay Postgres-vanilla: no partial index, no
 * {@code ENUM}, nothing H2's PostgreSQL compatibility mode can't emulate. That holds today, but
 * it's an assumption, not a guarantee — the first genuinely Postgres-specific migration could pass
 * here and still misbehave against real Postgres. Testcontainers would remove the caveat; that is
 * a separate, larger change (see root CLAUDE.md's "Out of Scope" list).
 */
@SpringBootTest(classes = SessionApplication.class)
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private SessionJpaRepository sessionRepository;

    @Autowired
    private SimulationJpaRepository simulationRepository;

    @Test
    void findsSimulationsForASessionOrderedByStartedAtDesc() {
        UUID sessionId = createSession();

        SimulationEntity simulation = newSimulation(sessionId, null);
        simulation.setFinishedAt(Instant.now());
        simulation.setStatus(GameState.X_WON);
        simulationRepository.save(simulation);

        List<SimulationEntity> found = simulationRepository.findBySessionIdOrderByStartedAtDesc(sessionId);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getSessionId()).isEqualTo(sessionId);
        assertThat(found.getFirst().getStatus()).isEqualTo(GameState.X_WON);
    }

    @Test
    void aSecondSimulationClaimingTheSameRunningSessionViolatesTheUniqueIndex() {
        UUID sessionId = createSession();
        UUID runningSessionId = UUID.randomUUID();
        simulationRepository.save(newSimulation(sessionId, runningSessionId));

        assertThatThrownBy(() -> simulationRepository.save(newSimulation(sessionId, runningSessionId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void multipleSimulationsWithNoRunningSessionAreAllowed() {
        UUID sessionId = createSession();

        assertThatCode(() -> {
            simulationRepository.save(newSimulation(sessionId, null));
            simulationRepository.save(newSimulation(sessionId, null));
        }).doesNotThrowAnyException();
    }

    private UUID createSession() {
        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash("test-owner-token-hash");
        sessionRepository.save(session);
        return sessionId;
    }

    private static SimulationEntity newSimulation(UUID sessionId, UUID runningSessionId) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId(UUID.randomUUID());
        simulation.setSessionId(sessionId);
        simulation.setErrorsCount(0);
        simulation.setStartedAt(Instant.now());
        simulation.setStatus(GameState.IN_PROGRESS);
        simulation.setRunningSessionId(runningSessionId);
        return simulation;
    }
}