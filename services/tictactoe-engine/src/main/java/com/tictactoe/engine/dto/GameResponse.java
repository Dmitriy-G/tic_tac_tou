package com.tictactoe.engine.dto;

import com.tictactoe.engine.domain.GameState;

public record GameResponse(String board, GameState state) {

}