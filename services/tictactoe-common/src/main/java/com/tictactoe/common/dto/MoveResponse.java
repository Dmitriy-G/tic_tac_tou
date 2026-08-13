package com.tictactoe.common.dto;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;

import java.util.List;

public record MoveResponse(List<String> board, GameState gameState, StepStatus stepStatus, Symbol winner) {

}
