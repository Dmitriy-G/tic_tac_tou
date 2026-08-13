package com.tictactoe.session.dto;

import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;

public record MoveRecord(int moveNumber, Symbol symbol, int position, StepStatus stepStatus) {
}
