package com.tictactoe.common.dto;

import com.tictactoe.common.domain.Symbol;

public record MoveRequest(Symbol symbol, int position) {
}
