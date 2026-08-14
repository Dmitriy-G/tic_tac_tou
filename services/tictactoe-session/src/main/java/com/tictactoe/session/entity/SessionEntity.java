package com.tictactoe.session.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    private UUID id;

    private String ownerTokenHash;

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
}