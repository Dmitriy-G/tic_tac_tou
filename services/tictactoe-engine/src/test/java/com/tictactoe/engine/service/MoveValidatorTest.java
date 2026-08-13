package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tictactoe.engine.util.BoardUtils.EMPTY_CELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterisation tests for the last-rule-wins validation cascade, written
 * BEFORE the MoveValidator extraction as the safety net for it.
 */
class MoveValidatorTest {

    private final MoveValidator moveValidator = new MoveValidator(new TurnPolicy());

    @Test
    void finishedGameWithOccupiedCellReportsCellOccupiedNotGameFinished() {
        List<String> board = emptyBoard();
        board.set(4, Symbol.X.name());

        StepStatus status = moveValidator.validate(board, GameState.X_WON, new MoveRequest(Symbol.O, 4));

        assertThat(status).isEqualTo(StepStatus.CELL_OCCUPIED);
    }

    @Test
    void finishedGameWithFreeCellReportsGameFinished() {
        List<String> board = emptyBoard();

        StepStatus status = moveValidator.validate(board, GameState.X_WON, new MoveRequest(Symbol.X, 0));

        assertThat(status).isEqualTo(StepStatus.GAME_FINISHED);
    }

    @Test
    void nullSymbolWithFreeCellAndValidPositionReportsInvalidSymbol() {
        List<String> board = emptyBoard();

        StepStatus status = moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(null, 0));

        assertThat(status).isEqualTo(StepStatus.INVALID_SYMBOL);
    }

    @Test
    void occupiedCellWithWrongTurnReportsOutOfTurnNotCellOccupied() {
        List<String> board = emptyBoard();
        board.set(0, Symbol.O.name());

        StepStatus status = moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(Symbol.X, 0));

        assertThat(status).isEqualTo(StepStatus.OUT_OF_TURN);
    }

    @Test
    void positionAboveRangeThrowsInsteadOfReturningInvalidPosition() {
        List<String> board = emptyBoard();

        assertThatThrownBy(() -> moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(Symbol.X, 9)))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void positionBelowRangeThrowsInsteadOfReturningInvalidPosition() {
        List<String> board = emptyBoard();

        assertThatThrownBy(() -> moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(Symbol.X, -1)))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void oAsTheFirstMoveReportsOutOfTurn() {
        List<String> board = emptyBoard();

        StepStatus status = moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(Symbol.O, 0));

        assertThat(status).isEqualTo(StepStatus.OUT_OF_TURN);
    }

    @Test
    void xOnAnEmptyBoardReportsCorrectStep() {
        List<String> board = emptyBoard();

        StepStatus status = moveValidator.validate(board, GameState.IN_PROGRESS, new MoveRequest(Symbol.X, 0));

        assertThat(status).isEqualTo(StepStatus.CORRECT_STEP);
    }

    private static List<String> emptyBoard() {
        List<String> board = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            board.add(EMPTY_CELL);
        }
        return board;
    }
}
