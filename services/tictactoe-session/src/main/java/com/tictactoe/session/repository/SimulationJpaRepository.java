package com.tictactoe.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SimulationJpaRepository extends JpaRepository<SimulationEntity, UUID> {

    List<SimulationEntity> findBySessionIdOrderByStartedAtDesc(UUID sessionId);
}