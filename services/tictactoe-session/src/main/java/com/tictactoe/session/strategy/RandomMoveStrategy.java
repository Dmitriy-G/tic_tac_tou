package com.tictactoe.session.strategy;

import com.tictactoe.session.util.BoardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomMoveStrategy implements MoveStrategy {

    private final Random random;

    public RandomMoveStrategy(Random random) {
        this.random = random;
    }

    @Override
    public int selectMove(List<String> board, String symbol) {
        List<Integer> emptyCells = new ArrayList<>();
        for (int i = 0; i < board.size(); i++) {
            if (BoardUtils.EMPTY_CELL.equals(board.get(i))) {
                emptyCells.add(i);
            }
        }
        if (emptyCells.isEmpty()) {
            throw new IllegalStateException("No empty cells left to move into");
        }
        return emptyCells.get(random.nextInt(emptyCells.size()));
    }
}