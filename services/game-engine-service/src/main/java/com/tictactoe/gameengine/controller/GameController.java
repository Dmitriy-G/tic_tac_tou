package com.tictactoe.gameengine.controller;

import com.tictactoe.gameengine.model.Game;
import com.tictactoe.gameengine.model.MoveRequest;
import com.tictactoe.gameengine.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/games")
@Tag(name = "Games", description = "Board state, move validation, and game outcome")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{gameId}/move")
    @Operation(summary = "Apply a move", description = "Validates and applies a move for the given game, returning the updated board and status.")
    public Game move(
            @Parameter(description = "Identifier of the game") @PathVariable String gameId,
            @RequestBody MoveRequest move) {
        return gameService.applyMove(gameId, move);
    }

    @GetMapping("/{gameId}")
    @Operation(summary = "Get game state", description = "Returns the current board and status for a game.")
    public Game getGame(@Parameter(description = "Identifier of the game") @PathVariable String gameId) {
        return gameService.getGame(gameId);
    }
}