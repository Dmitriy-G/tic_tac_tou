package com.tictactoe.session.entity;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    private UUID id;

    private String ownerTokenHash;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameState status;

    @Column(name = "board", nullable = false, length = 9)
    private String board;
    
    @Column(name = "winner", columnDefinition = "VARCHAR(1)")
    @Enumerated(EnumType.STRING)
    private Symbol winner;

    @Column(name = "errors_count", nullable = false)
    private int errorsCount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOwnerTokenHash() {
        return ownerTokenHash;
    }

    public void setOwnerTokenHash(String ownerTokenHash) {
        this.ownerTokenHash = ownerTokenHash;
    }

    public GameState getStatus() {
        return status;
    }

    public void setStatus(GameState status) {
        this.status = status;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public Symbol getWinner() {
        return winner;
    }

    public void setWinner(Symbol winner) {
        this.winner = winner;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}