package com.tictactoe.session.strategy;

import java.util.List;

public interface MoveStrategy {

    int selectMove(List<String> board, String symbol);
}