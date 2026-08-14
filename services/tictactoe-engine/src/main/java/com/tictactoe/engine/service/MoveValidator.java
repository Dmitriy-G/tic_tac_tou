package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MoveValidator {

    private final TurnPolicy turnPolicy;

    public MoveValidator(TurnPolicy turnPolicy) {
        this.turnPolicy = turnPolicy;
    }

    public StepStatus validate(List<String> board, GameState state, MoveRequest move) {
        StepStatus status = StepStatus.CORRECT_STEP;

        if (state != GameState.IN_PROGRESS) {
            status = StepStatus.GAME_FINISHED;
        }

        if (move.symbol() == null) {
            status = StepStatus.INVALID_SYMBOL;
        }

        if (move.position() < 0 || move.position() >= BoardUtils.BOARD_SIZE) {
            status = StepStatus.INVALID_POSITION;
        }

        if (!BoardUtils.EMPTY_CELL.equals(board.get(move.position()))) {
            status = StepStatus.CELL_OCCUPIED;
        }

        if (turnPolicy.isOutOfTurn(board, move.symbol())) {
            status = StepStatus.OUT_OF_TURN;
        }

        return status;
    }
}
