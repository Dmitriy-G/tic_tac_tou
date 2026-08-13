package com.tictactoe.session.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.session.entity.SimulationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimulationJpaRepository extends JpaRepository<SimulationEntity, UUID> {

    List<SimulationEntity> findBySessionIdOrderByStartedAtDesc(UUID sessionId);

    boolean existsBySessionIdAndStatus(UUID sessionId, GameState status);
}