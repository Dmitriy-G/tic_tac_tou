package com.tictactoe.session.domain;

import java.util.List;

public class GameSession {

    private String sessionId;
    private String gameId;
    private SessionStatus status;
    private List<MoveRecord> moveHistory;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public List<MoveRecord> getMoveHistory() {
        return moveHistory;
    }

    public void setMoveHistory(List<MoveRecord> moveHistory) {
        this.moveHistory = moveHistory;
    }
}