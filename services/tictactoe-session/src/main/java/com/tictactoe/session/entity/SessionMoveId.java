package com.tictactoe.session.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record SessionMoveId(UUID sessionId, short moveNumber) implements Serializable {
}