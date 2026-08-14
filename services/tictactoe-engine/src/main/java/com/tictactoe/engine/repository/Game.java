package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.engine.entity.GameEntity;
import com.tictactoe.engine.util.BoardUtils;

import java.util.List;

public final class Game {

    private final GameEntity entity;

    Game(GameEntity entity) {
        this.entity = entity;
    }

    public List<String> board() {
        return BoardUtils.convertToList(entity.getBoard());
    }

    public GameState state() {
        return entity.getState();
    }

    GameEntity entity() {
        return entity;
    }
}
