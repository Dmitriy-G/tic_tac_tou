package com.tictactoe.engine.service;

import com.tictactoe.common.domain.Symbol;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides whose turn it is to move on a given board.
 */
@Component
public class TurnPolicy {

    /**
     * True when it is NOT {@code symbol}'s turn to move on this board.
     */
    public boolean isOutOfTurn(List<String> board, Symbol symbol) {
        long emptyCount = board.stream().filter(BoardUtils.EMPTY_CELL::equals).count();

        // A full board is always out of turn. Reachable only when a full
        // board is somehow still IN_PROGRESS, which the outcome evaluator
        // prevents — kept as a defensive branch.
        if (emptyCount == 0) {
            return true;
        }

        return emptyCount % 2 == 0 ? Symbol.X.equals(symbol) : Symbol.O.equals(symbol);
    }
}
