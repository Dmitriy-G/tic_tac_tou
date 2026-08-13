package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationProgressTest {

    private static final List<String> INITIAL_BOARD = List.of("_", "_", "_", "_", "_", "_", "_", "_", "_");

    @Test
    void startsAtMoveOneWithSymbolX() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        assertThat(progress.moveNumber()).isEqualTo(1);
        assertThat(progress.errorsCount()).isEqualTo(0);
        assertThat(progress.gameState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(progress.currentSymbol()).isEqualTo("X");
        assertThat(progress.hasNextIteration()).isTrue();
        assertThat(progress.isFinished()).isFalse();
    }

    @Test
    void correctStepAdvancesMoveNumberAndAlternatesSymbol() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        progress.recordStep(correctStep(GameState.IN_PROGRESS));

        assertThat(progress.moveNumber()).isEqualTo(2);
        assertThat(progress.errorsCount()).isEqualTo(0);
        assertThat(progress.currentSymbol()).isEqualTo("O");
    }

    @Test
    void invalidStepIncrementsErrorsWithoutAdvancingMoveNumber() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        progress.recordStep(invalidStep());

        assertThat(progress.moveNumber()).isEqualTo(1);
        assertThat(progress.errorsCount()).isEqualTo(1);
        assertThat(progress.gameState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(progress.hasNextIteration()).isTrue();
    }

    @Test
    void eleventhInvalidStepForcesFailedState() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        for (int i = 0; i < 10; i++) {
            progress.recordStep(invalidStep());
        }
        assertThat(progress.gameState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(progress.hasNextIteration()).isTrue();

        progress.recordStep(invalidStep());

        assertThat(progress.errorsCount()).isEqualTo(11);
        assertThat(progress.gameState()).isEqualTo(GameState.FAILED);
        assertThat(progress.isFinished()).isTrue();
        assertThat(progress.hasNextIteration()).isFalse();
    }

    @Test
    void ninthCorrectStepEndsIterationEvenIfEngineStillReportsInProgress() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        for (int i = 0; i < 8; i++) {
            progress.recordStep(correctStep(GameState.IN_PROGRESS));
        }
        assertThat(progress.moveNumber()).isEqualTo(9);
        assertThat(progress.hasNextIteration()).isTrue();

        progress.recordStep(correctStep(GameState.IN_PROGRESS));

        assertThat(progress.moveNumber()).isEqualTo(10);
        assertThat(progress.gameState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(progress.hasNextIteration()).isFalse();
    }

    @Test
    void terminalGameStateFromEngineEndsIterationImmediately() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);

        progress.recordStep(correctStep(GameState.X_WON));

        assertThat(progress.gameState()).isEqualTo(GameState.X_WON);
        assertThat(progress.isFinished()).isTrue();
        assertThat(progress.hasNextIteration()).isFalse();
    }

    @Test
    void boardIsReplacedWithTheResponseBoard() {
        SimulationProgress progress = SimulationProgress.start(INITIAL_BOARD);
        List<String> newBoard = List.of("X", "_", "_", "_", "_", "_", "_", "_", "_");

        progress.recordStep(new MoveResponse(newBoard, GameState.IN_PROGRESS, StepStatus.CORRECT_STEP, null));

        assertThat(progress.board()).isEqualTo(newBoard);
    }

    private static MoveResponse correctStep(GameState gameState) {
        Symbol winner = gameState == GameState.X_WON ? Symbol.X : gameState == GameState.O_WON ? Symbol.O : null;
        return new MoveResponse(INITIAL_BOARD, gameState, StepStatus.CORRECT_STEP, winner);
    }

    private static MoveResponse invalidStep() {
        return new MoveResponse(INITIAL_BOARD, GameState.IN_PROGRESS, StepStatus.CELL_OCCUPIED, null);
    }
}