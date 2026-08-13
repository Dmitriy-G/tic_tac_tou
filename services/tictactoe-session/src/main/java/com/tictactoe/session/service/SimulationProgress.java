package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.dto.MoveResponse;

import java.util.List;

/**
 * Mutable state for one simulation run: the current board, game state, move number and error
 * count.
 *
 * <p>Termination proof: every {@link #recordStep} call increments exactly one of {@code
 * moveNumber} or {@code errorsCount}. {@code moveNumber} is capped at {@link #MAX_MOVES}, and
 * {@code errorsCount} forces a terminal state once it exceeds {@link #MAX_ERRORS}. So the loop
 * driven by {@link #hasNextIteration()} runs at most {@code MAX_MOVES + MAX_ERRORS + 1} = 20
 * iterations.
 */
final class SimulationProgress {

    private static final int MAX_MOVES = 9;
    private static final int MAX_ERRORS = 10;

    private List<String> board;
    private GameState gameState = GameState.IN_PROGRESS;
    private int moveNumber = 1;
    private int errorsCount = 0;

    private SimulationProgress(List<String> board) {
        this.board = board;
    }

    static SimulationProgress start(List<String> board) {
        return new SimulationProgress(board);
    }

    boolean hasNextIteration() {
        return !isFinished() && moveNumber <= MAX_MOVES;
    }

    String currentSymbol() {
        return moveNumber % 2 == 1 ? "X" : "O";
    }

    List<String> board() {
        return board;
    }

    int moveNumber() {
        return moveNumber;
    }

    int errorsCount() {
        return errorsCount;
    }

    GameState gameState() {
        return gameState;
    }

    boolean isFinished() {
        return gameState != GameState.IN_PROGRESS;
    }

    void recordStep(MoveResponse response) {
        board = response.board();
        gameState = response.gameState();
        if (!StepStatus.CORRECT_STEP.equals(response.stepStatus())) {
            errorsCount++;
            if (errorsCount > MAX_ERRORS) {
                gameState = GameState.FAILED;
            }
        } else {
            moveNumber++;
        }
    }
}