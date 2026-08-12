package com.tictactoe.session.strategy;

import java.util.List;
import java.util.Random;

public class RandomMoveStrategy implements MoveStrategy {

    private final Random random;

    public RandomMoveStrategy(Random random) {
        this.random = random;
    }

    @Override
    public int selectMove(List<String> board, String symbol) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}