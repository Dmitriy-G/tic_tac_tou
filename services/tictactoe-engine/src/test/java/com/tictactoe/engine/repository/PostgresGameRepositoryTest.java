package com.tictactoe.engine.repository;

import com.tictactoe.engine.EngineApplication;
import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.GameStatus;
import com.tictactoe.engine.domain.Symbol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EngineApplication.class)
@ActiveProfiles("test")
class PostgresGameRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void savesAndReloadsAGameInProgress() {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game();
        game.setGameId(gameId);
        game.setBoard(Arrays.asList("X", null, null, null, "O", null, null, null, null));
        game.setStatus(GameStatus.IN_PROGRESS);

        gameRepository.save(game);
        Optional<Game> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGameId()).isEqualTo(gameId);
        assertThat(reloaded.get().getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(reloaded.get().getWinner()).isNull();
        assertThat(reloaded.get().getBoard())
                .containsExactly("X", null, null, null, "O", null, null, null, null);
    }

    @Test
    void derivesWinnerFromWonState() {
        String gameId = UUID.randomUUID().toString();
        Game game = new Game();
        game.setGameId(gameId);
        game.setBoard(List.of("X", "X", "X", "O", "O", "", "", "", ""));
        game.setStatus(GameStatus.X_WON);

        gameRepository.save(game);
        Optional<Game> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getWinner()).isEqualTo(Symbol.X);
    }

    @Test
    void returnsEmptyForUnknownGameId() {
        assertThat(gameRepository.findById(UUID.randomUUID().toString())).isEmpty();
    }
}