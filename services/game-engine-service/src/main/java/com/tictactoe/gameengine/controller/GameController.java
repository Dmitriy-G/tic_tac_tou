package com.tictactoe.gameengine.controller;

import com.tictactoe.gameengine.model.Game;
import com.tictactoe.gameengine.model.MoveRequest;
import com.tictactoe.gameengine.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{gameId}/move")
    public Game move(@PathVariable String gameId, @RequestBody MoveRequest move) {
        return gameService.applyMove(gameId, move);
    }

    @GetMapping("/{gameId}")
    public Game getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }
}