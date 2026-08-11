package com.tictactoe.gamesession.model;

import java.util.List;

public class SessionEvent {

    private String sessionId;
    private List<String> board;
    private SessionStatus status;
    private String winner;

    public SessionEvent() {
    }

    public SessionEvent(String sessionId, List<String> board, SessionStatus status, String winner) {
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

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }
}