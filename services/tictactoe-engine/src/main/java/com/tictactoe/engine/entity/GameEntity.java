package com.tictactoe.engine.entity;

import com.tictactoe.common.domain.GameState;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    private UUID id;

    @Column(name = "board", nullable = false, length = 9)
    private String board;

    @Column(name = "state", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GameState state;

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

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}