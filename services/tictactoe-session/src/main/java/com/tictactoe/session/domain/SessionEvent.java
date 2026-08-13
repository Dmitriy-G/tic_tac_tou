package com.tictactoe.session.domain;

import java.util.List;

public class SessionEvent {

    private String sessionId;
    private List<String> board;
    private SimulationStatus status;
    private String winner;

    public SessionEvent() {
    }

    public SessionEvent(String sessionId, List<String> board, SimulationStatus status, String winner) {
        this.sessionId = sessionId;
        this.board = board;
        this.status = status;
        this.winner = winner;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public SimulationStatus getStatus() {
        return status;
    }

    public void setStatus(SimulationStatus status) {
        this.status = status;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}