package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.error.BadRequestException;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.engine.entity.GameEntity;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GameStore {

    private final GameJpaRepository gameRepository;

    public GameStore(GameJpaRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game load(String gameId) {
        GameEntity entity = gameRepository.findById(toUuid(gameId))
                .orElseThrow(() -> new GameNotFoundException(gameId));
        return new Game(entity);
    }

    public void create(String gameId, List<String> board) {
        GameEntity entity = new GameEntity();
        entity.setId(toUuid(gameId));
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(GameState.IN_PROGRESS);
        gameRepository.save(entity);
    }

    private static UUID toUuid(String gameId) {
        try {
            return UUID.fromString(gameId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_GAME_ID, "Malformed game id: " + gameId, e);
        }
    }

    public void save(Game game, List<String> board, GameState state) {
        GameEntity entity = game.entity();
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(state);
        gameRepository.save(entity);
    }
}
