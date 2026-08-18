package com.tictactoe.session.repository;

import com.tictactoe.session.SessionApplication;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.session.entity.SessionEntity;
import com.tictactoe.session.entity.SessionMoveEntity;
import com.tictactoe.session.entity.SessionMoveId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs on H2 in PostgreSQL compatibility mode ({@code jdbc:h2:mem:session_db;MODE=PostgreSQL} in
 * application-test.yml), not real Postgres — named accordingly. save()/findById() themselves are
 * Spring Data JPA framework behaviour and are not re-tested here; this class covers this
 * service's own query methods and the database-level guard in the V5 Flyway migration.
 *
 * <p>The old unique-partial-index guard on {@code simulations.running_session_id} is gone along
 * with the table itself — {@code claimForSimulation}'s conditional {@code UPDATE} is the entire
 * double-simulate guard now, and the tests here exercise it directly against H2 rather than
 * against a mock, the same way {@code GameStoreConcurrencyTest} exercises the engine's CAS.
 */
@SpringBootTest(classes = SessionApplication.class)
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private SessionJpaRepository sessionRepository;

    @Autowired
    private SessionMoveJpaRepository moveRepository;

    @Test
    void claimForSimulationSucceedsOnlyWhenTheSessionIsStillCreated() {
        UUID sessionId = createSession(GameState.CREATED);

        int claimed = sessionRepository.claimForSimulation(sessionId, Instant.now());

        assertThat(claimed).isEqualTo(1);
        assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus()).isEqualTo(GameState.IN_PROGRESS);
    }

    @Test
    void claimForSimulationFailsWhenTheSessionIsNoLongerCreated() {
        UUID sessionId = createSession(GameState.IN_PROGRESS);

        int claimed = sessionRepository.claimForSimulation(sessionId, Instant.now());

        assertThat(claimed).isEqualTo(0);
    }

    @Test
    void claimForSimulationFailsWhenTheSessionIsAlreadyTerminal() {
        UUID sessionId = createSession(GameState.DRAW);

        int claimed = sessionRepository.claimForSimulation(sessionId, Instant.now());

        assertThat(claimed).isEqualTo(0);
    }

    @Test
    void findsSessionMovesOrderedByMoveNumber() {
        UUID sessionId = createSession(GameState.IN_PROGRESS);
        saveMove(sessionId, 2, Symbol.O, 4);
        saveMove(sessionId, 1, Symbol.X, 0);

        List<SessionMoveEntity> moves = moveRepository.findBySessionIdOrderByMoveNumber(sessionId);

        assertThat(moves).hasSize(2);
        assertThat(moves.get(0).getId().moveNumber()).isEqualTo((short) 1);
        assertThat(moves.get(1).getId().moveNumber()).isEqualTo((short) 2);
    }

    private UUID createSession(GameState status) {
        UUID sessionId = UUID.randomUUID();
        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setOwnerTokenHash("test-owner-token-hash");
        session.setStatus(status);
        session.setBoard(".........");
        session.setErrorsCount(0);
        sessionRepository.save(session);
        return sessionId;
    }

    private void saveMove(UUID sessionId, int moveNumber, Symbol symbol, int position) {
        SessionMoveEntity move = new SessionMoveEntity();
        move.setId(new SessionMoveId(sessionId, (short) moveNumber));
        move.setSymbol(symbol);
        move.setPosition((short) position);
        move.setStepStatus(StepStatus.CORRECT_STEP);
        move.setCreatedAt(Instant.now());
        moveRepository.save(move);
    }
}