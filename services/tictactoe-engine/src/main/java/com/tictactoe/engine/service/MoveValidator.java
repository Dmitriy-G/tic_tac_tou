package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.engine.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates a move against the current board and game state.
 */
@Component
public class MoveValidator {

    private final TurnPolicy turnPolicy;

    public MoveValidator(TurnPolicy turnPolicy) {
        this.turnPolicy = turnPolicy;
    }

    /**
     * Precedence is deliberately LAST-RULE-WINS, i.e. the reverse of the
     * reading order below: OUT_OF_TURN &gt; CELL_OCCUPIED &gt; INVALID_POSITION
     * &gt; INVALID_SYMBOL &gt; GAME_FINISHED. Do not convert this to early
     * returns — that silently changes which status is reported when
     * several rules match at once (e.g. a finished game with an occupied
     * cell reports CELL_OCCUPIED, not GAME_FINISHED).
     */
    public StepStatus validate(List<String> board, GameState state, MoveRequest move) {
        StepStatus status = StepStatus.CORRECT_STEP;

        if (state != GameState.IN_PROGRESS) {
            status = StepStatus.GAME_FINISHED;
        }

        if (move.symbol() == null) {
            status = StepStatus.INVALID_SYMBOL;
        }

        // Currently UNREACHABLE in its effect: board.get(move.position())
        // below throws IndexOutOfBoundsException for an out-of-range
        // position before this status is ever observed by a caller. See
        // DEFERRED-2 in the notes at the bottom of GameServiceImpl.
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
