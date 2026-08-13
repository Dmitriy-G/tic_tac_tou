package com.tictactoe.session.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BoardUtils {

    public static final String EMPTY_CELL = ".";

    public static String convertToString(List<String> board) {
        return String.join("", board).replace("null", EMPTY_CELL);
    }

    public static List<String> convertToList(String board) {
        return Arrays.stream(board.split("")).collect(Collectors.toList());
    }
}
