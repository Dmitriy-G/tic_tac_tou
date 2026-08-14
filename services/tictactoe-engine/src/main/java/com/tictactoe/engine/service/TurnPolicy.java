package com.tictactoe.engine.service;

import com.tictactoe.common.domain.Symbol;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TurnPolicy {

    public boolean isOutOfTurn(List<String> board, Symbol symbol) {
        long emptyCount = board.stream().filter(BoardUtils.EMPTY_CELL::equals).count();

        if (emptyCount == 0) {
            return true;
        }

        return emptyCount % 2 == 0 ? Symbol.X.equals(symbol) : Symbol.O.equals(symbol);
    }
}
