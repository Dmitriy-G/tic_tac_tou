package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.engine.entity.GameEntity;
import com.tictactoe.engine.exception.GameNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStoreTest {

    @Mock
    private GameJpaRepository gameRepository;

    private GameStore gameStore;

    @Test
    void loadReturnsTheGameWrappingTheEntity() {
        gameStore = new GameStore(gameRepository);
        UUID id = UUID.randomUUID();
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setBoard("X...O....");
        entity.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(id)).thenReturn(Optional.of(entity));

        Game game = gameStore.load(id.toString());

        assertThat(game.state()).isEqualTo(GameState.IN_PROGRESS);
        assertThat(game.board()).containsExactly("X", ".", ".", ".", "O", ".", ".", ".", ".");
    }

    @Test
    void loadThrowsWhenGameIsUnknown() {
        gameStore = new GameStore(gameRepository);
        UUID id = UUID.randomUUID();
        when(gameRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameStore.load(id.toString()))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void createSavesANewInProgressEntity() {
        gameStore = new GameStore(gameRepository);
        UUID id = UUID.randomUUID();
        List<String> board = List.of(".", ".", ".", ".", ".", ".", ".", ".", ".");

        gameStore.create(id.toString(), board);

        ArgumentCaptor<GameEntity> captor = ArgumentCaptor.forClass(GameEntity.class);
        verify(gameRepository).save(captor.capture());
        GameEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getBoard()).isEqualTo(".........");
        assertThat(saved.getState()).isEqualTo(GameState.IN_PROGRESS);
    }

    @Test
    void saveReusesTheEntityLoadedEarlierWithoutASecondRead() {
        gameStore = new GameStore(gameRepository);
        UUID id = UUID.randomUUID();
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setBoard(".........");
        entity.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(id)).thenReturn(Optional.of(entity));

        Game game = gameStore.load(id.toString());
        gameStore.save(game, List.of("X", ".", ".", ".", ".", ".", ".", ".", "."), GameState.IN_PROGRESS);

        verify(gameRepository, times(1)).findById(any());
        verify(gameRepository).save(entity);
        assertThat(entity.getBoard()).isEqualTo("X........");
    }
}
