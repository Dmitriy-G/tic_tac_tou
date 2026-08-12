package com.tictactoe.engine.repository;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.GameStatus;
import com.tictactoe.engine.domain.Symbol;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresGameRepository implements GameRepository {

    private static final char EMPTY_CELL = '_';
    private static final int BOARD_SIZE = 9;

    private final GameJpaRepository gameJpaRepository;

    public PostgresGameRepository(GameJpaRepository gameJpaRepository) {
        this.gameJpaRepository = gameJpaRepository;
    }

    @Override
    public Game save(Game game) {
        GameEntity entity = new GameEntity();
        entity.setId(UUID.fromString(game.getGameId()));
        entity.setBoard(encodeBoard(game.getBoard()));
        entity.setState(game.getStatus().name());
        gameJpaRepository.save(entity);
        return game;
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return gameJpaRepository.findById(UUID.fromString(gameId)).map(this::toDomain);
    }

    private Game toDomain(GameEntity entity) {
        Game game = new Game();
        game.setGameId(entity.getId().toString());
        game.setBoard(decodeBoard(entity.getBoard()));
        GameStatus status = GameStatus.valueOf(entity.getState());
        game.setStatus(status);
        game.setWinner(switch (status) {
            case X_WON -> Symbol.X;
            case O_WON -> Symbol.O;
            case IN_PROGRESS, DRAW -> null;
        });
        return game;
    }

    private static String encodeBoard(List<String> board) {
        StringBuilder encoded = new StringBuilder(BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++) {
            String cell = board == null || i >= board.size() ? null : board.get(i);
            encoded.append(cell == null || cell.isBlank() ? EMPTY_CELL : cell.charAt(0));
        }
        return encoded.toString();
    }

    private static List<String> decodeBoard(String encoded) {
        List<String> board = new ArrayList<>(BOARD_SIZE);
        for (int i = 0; i < BOARD_SIZE; i++) {
            char cell = i < encoded.length() ? encoded.charAt(i) : EMPTY_CELL;
            board.add(cell == EMPTY_CELL ? null : String.valueOf(cell));
        }
        return board;
    }
}