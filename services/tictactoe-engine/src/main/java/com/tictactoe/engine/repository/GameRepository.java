package com.tictactoe.engine.repository;

import com.tictactoe.engine.domain.Game;

import java.util.Optional;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(String gameId);
}