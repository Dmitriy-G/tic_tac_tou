package com.tictactoe.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SimulationJpaRepository extends JpaRepository<SimulationEntity, UUID> {

    List<SimulationEntity> findBySessionIdOrderByStartedAtDesc(UUID sessionId);
}