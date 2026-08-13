package com.tictactoe.engine.dto;

import com.tictactoe.engine.domain.GameState;
import com.tictactoe.engine.domain.StepStatus;
import com.tictactoe.engine.domain.Symbol;

import java.util.List;

public record ApplyMoveResponse(List<String> board, GameState gameState, StepStatus stepStatus, Symbol winner) {

}
