package com.tictactoe.engine.domain;

import java.util.List;

public class Game {

    private String gameId;
    private List<String> board;
    private GameStatus status;
    private Symbol winner;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Symbol getWinner() {
        return winner;
    }

    public void setWinner(Symbol winner) {
        this.winner = winner;
    }
}