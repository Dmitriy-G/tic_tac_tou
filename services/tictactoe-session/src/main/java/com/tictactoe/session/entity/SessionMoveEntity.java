package com.tictactoe.session.entity;

import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "session_moves")
public class SessionMoveEntity {

    @EmbeddedId
    private SessionMoveId id;

    @Column(name = "symbol", nullable = false, columnDefinition = "VARCHAR(1)")
    @Enumerated(EnumType.STRING)
    private Symbol symbol;

    @Column(name = "position", nullable = false)
    private short position;

    @Column(name = "step_status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private StepStatus stepStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SessionMoveId getId() {
        return id;
    }

    public void setId(SessionMoveId id) {
        this.id = id;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    public short getPosition() {
        return position;
    }

    public void setPosition(short position) {
        this.position = position;
    }

    public StepStatus getStepStatus() {
        return stepStatus;
    }

    public void setStepStatus(StepStatus stepStatus) {
        this.stepStatus = stepStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}