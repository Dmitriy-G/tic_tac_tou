package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.engine.config.WinningLines;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameOutcomeEvaluator {

    private final WinningLines winningLines;

    public GameOutcomeEvaluator(WinningLines winningLines) {
        this.winningLines = winningLines;
    }

    public Outcome resolve(List<String> board, Symbol symbol) {
        if (isWin(board, symbol)) {
            GameState state = symbol == Symbol.X ? GameState.X_WON : GameState.O_WON;
            return new Outcome(state, symbol);
        }
        if (isFull(board)) {
            return new Outcome(GameState.DRAW, null);
        }
        return new Outcome(GameState.IN_PROGRESS, null);
    }

    private boolean isWin(List<String> board, Symbol symbol) {
        String mark = symbol.name();
        for (int[] line : winningLines.lines()) {
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

    public record Outcome(GameState state, Symbol winner) {
    }
}
