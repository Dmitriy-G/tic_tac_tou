package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.engine.config.WinningLines;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.tictactoe.engine.util.BoardUtils.EMPTY_CELL;
import static org.assertj.core.api.Assertions.assertThat;

class GameOutcomeEvaluatorTest {

    private static final String CONFIG = "0,1,2;3,4,5;6,7,8;0,3,6;1,4,7;2,5,8;0,4,8;2,4,6";

    private final GameOutcomeEvaluator evaluator = new GameOutcomeEvaluator(new WinningLines(CONFIG));

    static Stream<int[]> winningLines() {
        return Stream.of(
                new int[]{0, 1, 2}, new int[]{3, 4, 5}, new int[]{6, 7, 8},
                new int[]{0, 3, 6}, new int[]{1, 4, 7}, new int[]{2, 5, 8},
                new int[]{0, 4, 8}, new int[]{2, 4, 6}
        );
    }

    @ParameterizedTest
    @MethodSource("winningLines")
    void detectsAWinForXOnEveryLine(int[] line) {
        List<String> board = emptyBoard();
        for (int index : line) {
            board.set(index, Symbol.X.name());
        }

        GameOutcomeEvaluator.Outcome outcome = evaluator.resolve(board, Symbol.X);

        assertThat(outcome.state()).isEqualTo(GameState.X_WON);
        assertThat(outcome.winner()).isEqualTo(Symbol.X);
    }

    @ParameterizedTest
    @MethodSource("winningLines")
    void detectsAWinForOOnEveryLine(int[] line) {
        List<String> board = emptyBoard();
        for (int index : line) {
            board.set(index, Symbol.O.name());
        }

        GameOutcomeEvaluator.Outcome outcome = evaluator.resolve(board, Symbol.O);

        assertThat(outcome.state()).isEqualTo(GameState.O_WON);
        assertThat(outcome.winner()).isEqualTo(Symbol.O);
    }

    @Test
    void winOnTheNinthCellBeatsDraw() {
        // X's 9th move fills the board AND completes the bottom row (6,7,8).
        List<String> board = new ArrayList<>(List.of(
                "X", "O", "X",
                "O", "X", "O",
                "X", "X", EMPTY_CELL
        ));
        board.set(8, Symbol.X.name());

        GameOutcomeEvaluator.Outcome outcome = evaluator.resolve(board, Symbol.X);

        assertThat(outcome.state()).isEqualTo(GameState.X_WON);
        assertThat(outcome.winner()).isEqualTo(Symbol.X);
    }

    @Test
    void fullBoardWithNoLineIsADraw() {
        List<String> board = new ArrayList<>(List.of(
                "X", "O", "X",
                "X", "O", "O",
                "O", "X", "X"
        ));

        GameOutcomeEvaluator.Outcome outcome = evaluator.resolve(board, Symbol.X);

        assertThat(outcome.state()).isEqualTo(GameState.DRAW);
        assertThat(outcome.winner()).isNull();
    }

    @ParameterizedTest
    @EnumSource(Symbol.class)
    void boardWithNoWinAndFreeCellsIsInProgress(Symbol symbol) {
        List<String> board = emptyBoard();
        board.set(0, symbol.name());

        GameOutcomeEvaluator.Outcome outcome = evaluator.resolve(board, symbol);

        assertThat(outcome.state()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(outcome.winner()).isNull();
    }

    private static List<String> emptyBoard() {
        return new ArrayList<>(List.of(
                EMPTY_CELL, EMPTY_CELL, EMPTY_CELL,
                EMPTY_CELL, EMPTY_CELL, EMPTY_CELL,
                EMPTY_CELL, EMPTY_CELL, EMPTY_CELL
        ));
    }
}
