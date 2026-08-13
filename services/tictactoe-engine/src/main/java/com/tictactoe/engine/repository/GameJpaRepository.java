package com.tictactoe.engine.repository;

import com.tictactoe.engine.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {
}