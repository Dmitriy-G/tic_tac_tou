package com.tictactoe.session.entity;

import com.tictactoe.common.domain.GameState;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulations")
public class SimulationEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "errors_count", nullable = false)
    private int errorsCount;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameState status;

    @Column(name = "running_session_id")
    private UUID runningSessionId;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public int getErrorsCount() {
        return errorsCount;
    }

    public void setErrorsCount(int errorsCount) {
        this.errorsCount = errorsCount;
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

    public GameState getStatus() {
        return status;
    }

    public void setStatus(GameState status) {
        this.status = status;
    }

    public UUID getRunningSessionId() {
        return runningSessionId;
    }

    public void setRunningSessionId(UUID runningSessionId) {
        this.runningSessionId = runningSessionId;
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
}