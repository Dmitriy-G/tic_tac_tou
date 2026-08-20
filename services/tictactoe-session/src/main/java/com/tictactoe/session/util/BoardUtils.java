package com.tictactoe.session.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BoardUtils {

    public static final String EMPTY_CELL = ".";
    public static final int BOARD_SIZE = 9;

    public static String convertToString(List<String> board) {
        return String.join("", board);
    }

    public static List<String> convertToList(String board) {
        return Arrays.stream(board.split("")).collect(Collectors.toList());
    }

    public static List<String> emptyBoard() {
        return Collections.nCopies(BOARD_SIZE, EMPTY_CELL);
    }
}