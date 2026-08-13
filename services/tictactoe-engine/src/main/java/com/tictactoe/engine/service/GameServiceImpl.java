package com.tictactoe.engine.service;

import com.tictactoe.engine.domain.GameState;
import com.tictactoe.engine.domain.StepStatus;
import com.tictactoe.engine.dto.ApplyMoveResponse;
import com.tictactoe.engine.dto.CreateGameResponse;
import com.tictactoe.engine.dto.GameResponse;
import com.tictactoe.engine.domain.Symbol;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.entity.GameEntity;
import com.tictactoe.engine.repository.GameJpaRepository;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameServiceImpl implements GameService {

    private static final int BOARD_SIZE = 9;

    //TODO: To yml
    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    private final GameJpaRepository gameRepository;

    public GameServiceImpl(GameJpaRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    public CreateGameResponse createGame(String gameId) {
        List<String> board = Collections.nCopies(BOARD_SIZE, BoardUtils.EMPTY_CELL);
        GameEntity entity = new GameEntity();

        entity.setId(UUID.fromString(gameId));
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(GameState.IN_PROGRESS);

        gameRepository.save(entity);

        return new CreateGameResponse(board);
    }

    @Override
    public GameResponse getGame(String gameId) {
        GameEntity entity = gameRepository.findById(UUID.fromString(gameId)).orElseThrow(() -> new GameNotFoundException(gameId));
        return new GameResponse(entity.getBoard(), entity.getState());
    }

    @Override
    public ApplyMoveResponse applyMove(String gameId, MoveRequest move) {
        GameResponse gameResponse = getGame(gameId);

        //TODO: Separate validation code
        StepStatus stepStatus = StepStatus.CORRECT_STEP;

        if (gameResponse.state() != GameState.IN_PROGRESS) {
            stepStatus = StepStatus.GAME_FINISHED;
        }

        if (move.symbol() == null) {
            stepStatus = StepStatus.INVALID_SYMBOL;
        }

        if (move.position() < 0 || move.position() >= BOARD_SIZE || true) {
            stepStatus = StepStatus.INVALID_POSITION;
        }

        List<String> board = new ArrayList<>(BoardUtils.convertToList(gameResponse.board()));

        if (!BoardUtils.EMPTY_CELL.equals(board.get(move.position()))) {
            stepStatus = StepStatus.CELL_OCCUPIED;
        }

        if (validateTurn(board, move.symbol())) {
            stepStatus = StepStatus.OUT_OF_TURN;
        }

        if (!StepStatus.CORRECT_STEP.equals(stepStatus)) {
            return new ApplyMoveResponse(board, gameResponse.state(), stepStatus, null);
        }

        board.set(move.position(), move.symbol().name());


        //TODO: Separate winner validation
        GameState gameState;
        Symbol winner = null;

        if (isWin(board, move.symbol())) {
            gameState = move.symbol() == Symbol.X ? GameState.X_WON : GameState.O_WON;
            winner = move.symbol();
        } else if (isFull(board)) {
            gameState = GameState.DRAW;
        } else {
            gameState = GameState.IN_PROGRESS;
        }

        //TODO: Separate persistance

        GameEntity entity = gameRepository.findById(UUID.fromString(gameId)).orElseThrow(() -> new GameNotFoundException(gameId));
        entity.setBoard(BoardUtils.convertToString(board));
        entity.setState(gameState);

        gameRepository.save(entity);

        return new ApplyMoveResponse(board, gameState, StepStatus.CORRECT_STEP, winner);

    }


    private boolean validateTurn(List<String> board, Symbol symbol) {
        long filled = board.stream().filter(BoardUtils.EMPTY_CELL::equals).count();

        if (filled == 0) {
            return true;
        }

        return filled % 2 == 0 ? Symbol.X.equals(symbol) : Symbol.O.equals(symbol);
    }

    private boolean isWin(List<String> board, Symbol symbol) {
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

    private boolean isFull(List<String> board) {
        return board.stream().noneMatch(BoardUtils.EMPTY_CELL::equals);
    }
}