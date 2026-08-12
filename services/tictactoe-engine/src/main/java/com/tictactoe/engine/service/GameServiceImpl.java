package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.domain.GameStatus;
import com.tictactoe.engine.domain.Symbol;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.exception.InvalidMoveException;
import com.tictactoe.engine.repository.GameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameServiceImpl implements GameService {

    private static final int BOARD_SIZE = 9;
    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final GameRepository gameRepository;
    private final Map<String, Object> gameLocks = new ConcurrentHashMap<>();

    public GameServiceImpl(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public Game createGame(String gameId) {
        Game game = new Game();
        game.setGameId(gameId);
        game.setBoard(new ArrayList<>(Collections.nCopies(BOARD_SIZE, null)));
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setWinner(null);
        return gameRepository.save(game);
    }

    @Override
    public Game getGame(String gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Override
    public Game applyMove(String gameId, MoveRequest move) {
        synchronized (lockFor(gameId)) {
            Game game = getGame(gameId);

            if (game.getStatus() != GameStatus.IN_PROGRESS) {
                throw new InvalidMoveException("GAME_FINISHED", HttpStatus.CONFLICT,
                        "Game " + gameId + " is already finished");
            }
            if (move.getSymbol() == null) {
                throw new InvalidMoveException("INVALID_SYMBOL", HttpStatus.BAD_REQUEST,
                        "Symbol must be X or O");
            }
            if (move.getPosition() < 0 || move.getPosition() >= BOARD_SIZE) {
                throw new InvalidMoveException("INVALID_POSITION", HttpStatus.BAD_REQUEST,
                        "Position must be between 0 and " + (BOARD_SIZE - 1));
            }

            List<String> board = game.getBoard();
            if (board.get(move.getPosition()) != null) {
                throw new InvalidMoveException("CELL_OCCUPIED", HttpStatus.CONFLICT,
                        "Cell " + move.getPosition() + " is already occupied");
            }

            Symbol expectedTurn = nextTurn(board);
            if (move.getSymbol() != expectedTurn) {
                throw new InvalidMoveException("OUT_OF_TURN", HttpStatus.CONFLICT,
                        "It is not " + move.getSymbol() + "'s turn");
            }

            board.set(move.getPosition(), move.getSymbol().name());

            if (isWin(board, move.getSymbol())) {
                game.setStatus(move.getSymbol() == Symbol.X ? GameStatus.X_WON : GameStatus.O_WON);
                game.setWinner(move.getSymbol());
            } else if (isFull(board)) {
                game.setStatus(GameStatus.DRAW);
            } else {
                game.setStatus(GameStatus.IN_PROGRESS);
            }

            return gameRepository.save(game);
        }
    }

    private Object lockFor(String gameId) {
        return gameLocks.computeIfAbsent(gameId, id -> new Object());
    }

    private static Symbol nextTurn(List<String> board) {
        long filled = board.stream().filter(cell -> cell != null).count();
        return filled % 2 == 0 ? Symbol.X : Symbol.O;
    }

    private static boolean isWin(List<String> board, Symbol symbol) {
        String mark = symbol.name();
        for (int[] line : WINNING_LINES) {
            if (mark.equals(board.get(line[0]))
                    && mark.equals(board.get(line[1]))
                    && mark.equals(board.get(line[2]))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(List<String> board) {
        return board.stream().noneMatch(cell -> cell == null);
    }
}