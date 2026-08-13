package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.engine.entity.GameEntity;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The two repository touchpoints applyMove needs, so {@code GameServiceImpl}
 * never sees {@link GameEntity}, {@link GameJpaRepository} or {@link UUID}.
 */
@Component
public class GameStore {

    private final GameJpaRepository gameRepository;

    public GameStore(GameJpaRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game load(String gameId) {
        GameEntity entity = gameRepository.findById(UUID.fromString(gameId))
                .orElseThrow(() -> new GameNotFoundException(gameId));
        return new Game(entity);
    }

    public void create(String gameId, List<String> board) {
        GameEntity entity = new GameEntity();
        entity.setId(UUID.fromString(gameId));
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(GameState.IN_PROGRESS);
        gameRepository.save(entity);
    }

    /**
     * Persists {@code board}/{@code state} onto the entity {@code game} was
     * loaded from — no second {@code findById}. Reuses the row read by the
     * {@link #load} call that produced {@code game}, collapsing what used to
     * be two repository reads and one write into one read and one write.
     */
    public void save(Game game, List<String> board, GameState state) {
        GameEntity entity = game.entity();
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(state);
        gameRepository.save(entity);
    }
}
