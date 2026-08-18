package com.tictactoe.engine.service;

import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.GameResponse;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.error.ConflictException;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.engine.repository.Game;
import com.tictactoe.engine.repository.GameStore;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {

    private static final int MAX_ATTEMPTS = 3;

    private final GameStore gameStore;
    private final MoveValidator moveValidator;
    private final GameOutcomeEvaluator outcomeEvaluator;

    public GameService(GameStore gameStore, MoveValidator moveValidator, GameOutcomeEvaluator outcomeEvaluator) {
        this.gameStore = gameStore;
        this.moveValidator = moveValidator;
        this.outcomeEvaluator = outcomeEvaluator;
    }

    public CreateGameResponse createGame(String gameId) {
        List<String> board = BoardUtils.emptyBoard();
        gameStore.create(gameId, board);
        return new CreateGameResponse(board);
    }

    public GameResponse getGame(String gameId) {
        Game game = gameStore.load(gameId);
        return new GameResponse(BoardUtils.convertToString(game.board()), game.state());
    }

    public MoveResponse applyMove(String gameId, MoveRequest move) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Game game = gameStore.load(gameId);
            List<String> board = new ArrayList<>(game.board());

            StepStatus status = moveValidator.validate(board, game.state(), move);
            if (status != StepStatus.CORRECT_STEP) {
                return new MoveResponse(board, game.state(), status, null);
            }

            board.set(move.position(), move.symbol().name());
            GameOutcomeEvaluator.Outcome outcome = outcomeEvaluator.resolve(board, move.symbol());

            if (gameStore.compareAndSave(game, board, outcome.state())) {
                return new MoveResponse(board, outcome.state(), StepStatus.CORRECT_STEP, outcome.winner());
            }
            // Another move landed between our read and our write. Loop: the next iteration
            // re-reads the winning thread's board, and the validator rejects this move with
            // the honest reason — CELL_OCCUPIED or OUT_OF_TURN.
        }
        throw new ConflictException(ErrorCode.CONFLICT,
                "Move on game " + gameId + " lost " + MAX_ATTEMPTS + " compare-and-swap attempts");
    }
}
