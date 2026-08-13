package com.tictactoe.engine.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinningLinesTest {

    private static final String VALID_CONFIG = "0,1,2;3,4,5;6,7,8;0,3,6;1,4,7;2,5,8;0,4,8;2,4,6";

    @Test
    void parsesAllEightLines() {
        WinningLines winningLines = new WinningLines(VALID_CONFIG);

        assertThat(winningLines.lines().length).isEqualTo(8);
        assertThat(winningLines.lines()[0]).containsExactly(0, 1, 2);
        assertThat(winningLines.lines()[7]).containsExactly(2, 4, 6);
    }

    @Test
    void rejectsEmptyConfig() {
        assertThatThrownBy(() -> new WinningLines(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonNumericIndex() {
        assertThatThrownBy(() -> new WinningLines("0,1,x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsLineWithWrongLength() {
        assertThatThrownBy(() -> new WinningLines("0,1,2,3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 3");
    }

    @Test
    void rejectsLineWithTooFewIndices() {
        assertThatThrownBy(() -> new WinningLines("0,1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly 3");
    }

    @Test
    void rejectsIndexBelowRange() {
        assertThatThrownBy(() -> new WinningLines("-1,1,2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("0..8");
    }

    @Test
    void rejectsIndexAboveRange() {
        assertThatThrownBy(() -> new WinningLines("0,1,9"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("0..8");
    }
}
