package com.tictactoe.common.dto;

import com.tictactoe.common.domain.GameState;

public record GameResponse(String board, GameState state) {

}
