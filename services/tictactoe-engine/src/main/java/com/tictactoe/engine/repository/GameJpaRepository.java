package com.tictactoe.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {
}