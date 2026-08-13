package com.tictactoe.session.client;

import com.tictactoe.session.domain.GameState;
import com.tictactoe.session.domain.StepStatus;
import com.tictactoe.session.domain.Symbol;

import java.util.List;

//TODO: Duplicate in both services, need library for common data
public record ApplyMoveResponse(List<String> board, GameState gameState, StepStatus stepStatus, Symbol winner) {

}