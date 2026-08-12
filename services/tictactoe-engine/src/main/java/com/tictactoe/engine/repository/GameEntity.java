package com.tictactoe.engine.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    private UUID id;

    @Column(name = "board", nullable = false, length = 9)
    private String board;

    @Column(name = "state", nullable = false, length = 20)
    private String state;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}