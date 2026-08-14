package com.tictactoe.engine.config;

import com.tictactoe.engine.util.BoardUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class WinningLines {

    private static final int LINE_LENGTH = 3;

    private final int[][] lines;

    public WinningLines(@Value("${game.winning-lines}") String config) {
        this.lines = parse(config);
        validate(this.lines);
    }

    public int[][] lines() {
        return lines;
    }

    private static int[][] parse(String config) {
        try {
            return Arrays.stream(config.split(";"))
                    .map(line -> Arrays.stream(line.split(","))
                            .mapToInt(Integer::parseInt)
                            .toArray())
                    .toArray(int[][]::new);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("game.winning-lines contains a non-numeric index: " + config, e);
        }
    }

    private static void validate(int[][] lines) {
        if (lines.length == 0) {
            throw new IllegalStateException("game.winning-lines must define at least one line");
        }
        for (int[] line : lines) {
            if (line.length != LINE_LENGTH) {
                throw new IllegalStateException(
                        "Each winning line must have exactly " + LINE_LENGTH + " indices, got: " + Arrays.toString(line));
            }
            for (int index : line) {
                if (index < 0 || index >= BoardUtils.BOARD_SIZE) {
                    throw new IllegalStateException(
                            "Winning line index must be in 0.." + (BoardUtils.BOARD_SIZE - 1) + ", got: " + index);
                }
            }
        }
    }
}
