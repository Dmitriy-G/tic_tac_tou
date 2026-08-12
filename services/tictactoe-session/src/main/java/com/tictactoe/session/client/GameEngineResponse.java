package com.tictactoe.session.client;

import java.util.List;

public class GameEngineResponse {

    private String gameId;
    private List<String> board;
    private String status;
    private String winner;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}