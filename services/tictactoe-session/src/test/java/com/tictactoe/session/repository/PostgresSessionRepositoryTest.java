package com.tictactoe.session.repository;

import com.tictactoe.session.SessionApplication;
import com.tictactoe.session.domain.SimulationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SessionApplication.class)
@ActiveProfiles("test")
class PostgresSessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SimulationRepository simulationRepository;

    @Test
    void savesAndReloadsASessionIdentity() {
        String sessionId = UUID.randomUUID().toString();
        GameSession session = new GameSession();
        session.setSessionId(sessionId);
        session.setGameId(sessionId);
        session.setStatus(SimulationStatus.CREATED);
        session.setMoveHistory(new ArrayList<>());

        sessionRepository.save(session);
        Optional<GameSession> reloaded = sessionRepository.findById(sessionId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void returnsEmptyForUnknownSessionId() {
        assertThat(sessionRepository.findById(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void findsTheLatestSimulationForASession() {
        String sessionId = UUID.randomUUID().toString();
        GameSession session = new GameSession();
        session.setSessionId(sessionId);
        session.setGameId(sessionId);
        session.setStatus(SimulationStatus.CREATED);
        session.setMoveHistory(new ArrayList<>());
        sessionRepository.save(session);

        Simulation simulation = new Simulation();
        simulation.setId(UUID.randomUUID().toString());
        simulation.setSessionId(sessionId);
        simulation.setErrorsCount(0);
        simulation.setStartedAt(Instant.now());
        simulation.setFinishedAt(Instant.now());
        simulation.setStatus(SimulationStatus.X_WON);
        simulationRepository.save(simulation);

        Optional<Simulation> latest = simulationRepository.findLatestBySessionId(sessionId);

        assertThat(latest).isPresent();
        assertThat(latest.get().getSessionId()).isEqualTo(sessionId);
        assertThat(latest.get().getStatus()).isEqualTo(SimulationStatus.X_WON);
    }
}