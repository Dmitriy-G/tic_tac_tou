package com.tictactoe.engine.repository;

import com.tictactoe.engine.domain.Game;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        games.put(game.getGameId(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(String gameId) {
        return Optional.ofNullable(games.get(gameId));
    }
}