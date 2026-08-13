package com.tictactoe.engine.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.CreateGameResponse;
import com.tictactoe.common.dto.GameResponse;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.engine.repository.Game;
import com.tictactoe.engine.repository.GameStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameStore gameStore;
    @Mock
    private MoveValidator moveValidator;
    @Mock
    private GameOutcomeEvaluator outcomeEvaluator;

    private GameService service() {
        return new GameService(gameStore, moveValidator, outcomeEvaluator);
    }

    @Test
    void createGameDelegatesToGameStoreAndReturnsTheEmptyBoard() {
        CreateGameResponse response = service().createGame("game-1");

        assertThat(response.board()).containsExactly(".", ".", ".", ".", ".", ".", ".", ".", ".");
        verify(gameStore).create(eq("game-1"), anyList());
    }

    @Test
    void getGameMapsTheLoadedGameToAGameResponse() {
        Game game = mockGame(List.of("X", ".", ".", ".", ".", ".", ".", ".", "."), GameState.IN_PROGRESS);
        when(gameStore.load("game-1")).thenReturn(game);

        GameResponse response = service().getGame("game-1");

        assertThat(response.board()).isEqualTo("X........");
        assertThat(response.state()).isEqualTo(GameState.IN_PROGRESS);
    }

    @Test
    void rejectedMoveReturnsTheUnchangedBoardAndStateWithoutPersisting() {
        List<String> boardBefore = List.of("X", ".", ".", ".", ".", ".", ".", ".", ".");
        Game game = mockGame(boardBefore, GameState.IN_PROGRESS);
        when(gameStore.load("game-1")).thenReturn(game);
        MoveRequest move = new MoveRequest(Symbol.O, 0);
        when(moveValidator.validate(anyList(), eq(GameState.IN_PROGRESS), eq(move)))
                .thenReturn(StepStatus.CELL_OCCUPIED);

        MoveResponse response = service().applyMove("game-1", move);

        assertThat(response.board()).isEqualTo(boardBefore);
        assertThat(response.gameState()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(response.stepStatus()).isEqualTo(StepStatus.CELL_OCCUPIED);
        assertThat(response.winner()).isNull();
        verify(gameStore, never()).save(any(), anyList(), any());
    }

    @Test
    void acceptedMovePersistsTheNewBoardAndReturnsTheOutcome() {
        List<String> boardBefore = List.of(".", ".", ".", ".", ".", ".", ".", ".", ".");
        Game game = mockGame(boardBefore, GameState.IN_PROGRESS);
        when(gameStore.load("game-1")).thenReturn(game);
        MoveRequest move = new MoveRequest(Symbol.X, 0);
        when(moveValidator.validate(anyList(), eq(GameState.IN_PROGRESS), eq(move)))
                .thenReturn(StepStatus.CORRECT_STEP);
        when(outcomeEvaluator.resolve(anyList(), eq(Symbol.X)))
                .thenReturn(new GameOutcomeEvaluator.Outcome(GameState.IN_PROGRESS, null));

        MoveResponse response = service().applyMove("game-1", move);

        assertThat(response.board().get(0)).isEqualTo("X");
        assertThat(response.stepStatus()).isEqualTo(StepStatus.CORRECT_STEP);
        assertThat(response.gameState()).isEqualTo(GameState.IN_PROGRESS);
        verify(gameStore).save(eq(game), anyList(), eq(GameState.IN_PROGRESS));
    }

    private static Game mockGame(List<String> board, GameState state) {
        Game game = org.mockito.Mockito.mock(Game.class);
        when(game.board()).thenReturn(board);
        when(game.state()).thenReturn(state);
        return game;
    }
}
