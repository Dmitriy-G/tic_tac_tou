package com.tictactoe.session.repository;

import com.tictactoe.session.domain.Simulation;

import java.util.Optional;

public interface SimulationRepository {

    Simulation save(Simulation simulation);

    Optional<Simulation> findLatestBySessionId(String sessionId);
}