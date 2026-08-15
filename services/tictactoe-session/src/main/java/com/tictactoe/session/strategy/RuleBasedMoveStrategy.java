package com.tictactoe.session.strategy;

import com.tictactoe.session.util.BoardUtils;

import java.util.List;

public class RuleBasedMoveStrategy implements MoveStrategy {

    private static final int[][] WINNING_LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6},
    };
    private static final int CENTER = 4;
    private static final int[] CORNERS = {0, 2, 6, 8};
    private static final int[] SIDES = {1, 3, 5, 7};

    @Override
    public int selectMove(List<String> board, String symbol) {
        Integer winningMove = findWinningMove(board, symbol);
        if (winningMove != null) {
            return winningMove;
        }
        Integer blockingMove = findWinningMove(board, opponentOf(symbol));
        if (blockingMove != null) {
            return blockingMove;
        }
        if (isEmpty(board, CENTER)) {
            return CENTER;
        }
        Integer corner = firstEmpty(board, CORNERS);
        if (corner != null) {
            return corner;
        }
        Integer side = firstEmpty(board, SIDES);
        if (side != null) {
            return side;
        }
        throw new IllegalStateException("No empty cells left to move into");
    }

    private static Integer findWinningMove(List<String> board, String symbol) {
        for (int[] line : WINNING_LINES) {
            int symbolCount = 0;
            Integer emptyIndex = null;
            for (int index : line) {
                String cell = board.get(index);
                if (symbol.equals(cell)) {
                    symbolCount++;
                } else if (BoardUtils.EMPTY_CELL.equals(cell)) {
                    emptyIndex = index;
                }
            }
            if (symbolCount == 2 && emptyIndex != null) {
                return emptyIndex;
            }
        }
        return null;
    }

    private static Integer firstEmpty(List<String> board, int[] candidates) {
        for (int index : candidates) {
            if (isEmpty(board, index)) {
                return index;
            }
        }
        return null;
    }

    private static boolean isEmpty(List<String> board, int index) {
        return BoardUtils.EMPTY_CELL.equals(board.get(index));
    }

    private static String opponentOf(String symbol) {
        return "X".equals(symbol) ? "O" : "X";
    }
}
