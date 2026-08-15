package com.tictactoe.session.strategy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedMoveStrategyTest {

    private final RuleBasedMoveStrategy strategy = new RuleBasedMoveStrategy();

    @Test
    void takesTheWinningMoveOverEverythingElse() {
        List<String> board = List.of("X", "X", ".", "O", "O", ".", ".", ".", ".");

        assertThat(strategy.selectMove(board, "X")).isEqualTo(2);
    }

    @Test
    void blocksTheOpponentsWinningMoveWhenItCannotWinItself() {
        List<String> board = List.of("O", "O", ".", "X", ".", ".", ".", ".", ".");

        assertThat(strategy.selectMove(board, "X")).isEqualTo(2);
    }

    @Test
    void takesTheCenterWhenNoWinOrBlockIsAvailable() {
        List<String> board = List.of(".", ".", ".", ".", ".", ".", ".", ".", ".");

        assertThat(strategy.selectMove(board, "X")).isEqualTo(4);
    }

    @Test
    void takesAFreeCornerWhenTheCenterIsTaken() {
        List<String> board = List.of(".", ".", ".", ".", "X", ".", ".", ".", ".");

        assertThat(strategy.selectMove(board, "O")).isEqualTo(0);
    }

    @Test
    void takesAFreeSideWhenCenterAndCornersAreTaken() {
        List<String> board = List.of("X", ".", "O", ".", "O", ".", "O", ".", "X");

        assertThat(strategy.selectMove(board, "X")).isEqualTo(1);
    }

    @Test
    void throwsWhenNoEmptyCellsRemain() {
        List<String> board = List.of("X", "O", "X", "X", "O", "O", "O", "X", "X");

        assertThatThrownBy(() -> strategy.selectMove(board, "X")).isInstanceOf(IllegalStateException.class);
    }
}
