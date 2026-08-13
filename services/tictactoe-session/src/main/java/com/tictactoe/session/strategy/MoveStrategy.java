package com.tictactoe.session.strategy;

import java.util.List;

//TODO: need strategy for error, it's should break rules
public interface MoveStrategy {

    int selectMove(List<String> board, String symbol);
}