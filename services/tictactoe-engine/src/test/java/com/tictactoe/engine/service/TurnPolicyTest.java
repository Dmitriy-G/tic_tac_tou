package com.tictactoe.engine.service;

import com.tictactoe.common.domain.Symbol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tictactoe.engine.util.BoardUtils.EMPTY_CELL;
import static org.assertj.core.api.Assertions.assertThat;

class TurnPolicyTest {

    private final TurnPolicy turnPolicy = new TurnPolicy();

    @Test
    void xIsNotOutOfTurnOnAnEmptyBoard() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(0), Symbol.X)).isFalse();
    }

    @Test
    void oIsOutOfTurnOnAnEmptyBoard() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(0), Symbol.O)).isTrue();
    }

    @Test
    void oIsNotOutOfTurnAfterOneMove() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(1), Symbol.O)).isFalse();
    }

    @Test
    void xIsOutOfTurnAfterOneMove() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(1), Symbol.X)).isTrue();
    }

    @Test
    void xIsNotOutOfTurnAfterTwoMoves() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(2), Symbol.X)).isFalse();
    }

    @Test
    void oIsOutOfTurnAfterTwoMoves() {
        assertThat(turnPolicy.isOutOfTurn(boardWithMoves(2), Symbol.O)).isTrue();
    }

    @Test
    void aFullBoardIsAlwaysOutOfTurn() {
        List<String> full = boardWithMoves(9);

        assertThat(turnPolicy.isOutOfTurn(full, Symbol.X)).isTrue();
        assertThat(turnPolicy.isOutOfTurn(full, Symbol.O)).isTrue();
    }

    private static List<String> boardWithMoves(int movesMade) {
        List<String> board = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            board.add(i < movesMade ? "X" : EMPTY_CELL);
        }
        return board;
    }
}
