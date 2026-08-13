package com.tictactoe.engine.dto;

import com.tictactoe.engine.domain.Symbol;

public record MoveRequest(Symbol symbol, int position) {
}