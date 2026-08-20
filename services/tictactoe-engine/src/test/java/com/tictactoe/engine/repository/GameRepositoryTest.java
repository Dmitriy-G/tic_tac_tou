package com.tictactoe.engine.repository;

import com.tictactoe.engine.EngineApplication;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.engine.entity.GameEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Runs on H2 in PostgreSQL compatibility mode (application-test.yml), not real Postgres — named
// accordingly.
@SpringBootTest(classes = EngineApplication.class)
@ActiveProfiles("test")
class GameRepositoryTest {

    @Autowired
    private GameJpaRepository gameRepository;

    @Test
    void savesAndReloadsAGameInProgress() {
        UUID gameId = UUID.randomUUID();
        GameEntity entity = new GameEntity();
        entity.setId(gameId);
        entity.setBoard("X...O....");
        entity.setState(GameState.IN_PROGRESS);

        gameRepository.save(entity);
        Optional<GameEntity> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getId()).isEqualTo(gameId);
        assertThat(reloaded.get().getState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(reloaded.get().getBoard()).isEqualTo("X...O....");
    }

    @Test
    void savesAndReloadsAWonGame() {
        UUID gameId = UUID.randomUUID();
        GameEntity entity = new GameEntity();
        entity.setId(gameId);
        entity.setBoard("XXXOO....");
        entity.setState(GameState.X_WON);

        gameRepository.save(entity);
        Optional<GameEntity> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getState()).isEqualTo(GameState.X_WON);
    }

    @Test
    void returnsEmptyForUnknownGameId() {
        assertThat(gameRepository.findById(UUID.randomUUID())).isEmpty();
    }
}
