package com.tictactoe.engine.repository;

import com.tictactoe.engine.EngineApplication;
import com.tictactoe.engine.domain.GameState;
import com.tictactoe.engine.dto.GameResponse;
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
class PostgresGameResponseRepositoryTest {

    @Autowired
    private GameRepository gameRepository;

    @Test
    void savesAndReloadsAGameInProgress() {
        String gameId = UUID.randomUUID().toString();
        GameResponse gameResponse = new GameResponse();
        gameResponse.setGameId(gameId);
        gameResponse.setBoard(Arrays.asList("X", null, null, null, "O", null, null, null, null));
        gameResponse.setStatus(GameState.IN_PROGRESS);

        gameRepository.save(gameResponse);
        Optional<GameResponse> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGameId()).isEqualTo(gameId);
        assertThat(reloaded.get().getStatus()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(reloaded.get().getWinner()).isNull();
        assertThat(reloaded.get().getBoard())
                .containsExactly("X", null, null, null, "O", null, null, null, null);
    }

    @Test
    void derivesWinnerFromWonState() {
        String gameId = UUID.randomUUID().toString();
        GameResponse gameResponse = new GameResponse();
        gameResponse.setGameId(gameId);
        gameResponse.setBoard(List.of("X", "X", "X", "O", "O", "", "", "", ""));
        gameResponse.setStatus(GameState.X_WON);

        gameRepository.save(gameResponse);
        Optional<GameResponse> reloaded = gameRepository.findById(gameId);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getWinner()).isEqualTo(Symbol.X);
    }

    @Test
    void returnsEmptyForUnknownGameId() {
        assertThat(gameRepository.findById(UUID.randomUUID().toString())).isEmpty();
    }
}