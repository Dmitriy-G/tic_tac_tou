package com.tictactoe.session.client;


import com.tictactoe.session.domain.GameState;

public record GameResponse(String board, GameState state) {

}