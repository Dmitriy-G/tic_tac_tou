package com.tictactoe.engine.controller;

import com.tictactoe.engine.domain.Game;
import com.tictactoe.engine.dto.CreateGameRequest;
import com.tictactoe.engine.dto.MoveRequest;
import com.tictactoe.engine.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/games")
@Tag(name = "Games", description = "Board state, move validation, and game outcome")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a game", description = "Creates a new game and returns its initial board and status.")
    public Game createGame(@RequestBody(required = false) CreateGameRequest request) {
        String gameId = request != null && request.gameId() != null && !request.gameId().isBlank()
                ? request.gameId()
                : UUID.randomUUID().toString();
        return gameService.createGame(gameId);
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