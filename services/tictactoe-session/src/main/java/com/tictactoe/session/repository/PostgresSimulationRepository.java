package com.tictactoe.session.repository;

import com.tictactoe.session.domain.Simulation;
import com.tictactoe.session.domain.SessionStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresSimulationRepository implements SimulationRepository {

    private final SimulationJpaRepository simulationJpaRepository;

    public PostgresSimulationRepository(SimulationJpaRepository simulationJpaRepository) {
        this.simulationJpaRepository = simulationJpaRepository;
    }

    @Override
    public Simulation save(Simulation simulation) {
        SimulationEntity entity = new SimulationEntity();
        entity.setId(UUID.fromString(simulation.getId()));
        entity.setSessionId(UUID.fromString(simulation.getSessionId()));
        entity.setErrorsCount(simulation.getErrorsCount());
        entity.setStartedAt(simulation.getStartedAt());
        entity.setFinishedAt(simulation.getFinishedAt());
        entity.setStatus(simulation.getStatus().name());
        simulationJpaRepository.save(entity);
        return simulation;
    }

    @Override
    public Optional<Simulation> findLatestBySessionId(String sessionId) {
        List<SimulationEntity> simulations =
                simulationJpaRepository.findBySessionIdOrderByStartedAtDesc(UUID.fromString(sessionId));
        return simulations.stream().findFirst().map(this::toDomain);
    }

    private Simulation toDomain(SimulationEntity entity) {
        Simulation simulation = new Simulation();
        simulation.setId(entity.getId().toString());
        simulation.setSessionId(entity.getSessionId().toString());
        simulation.setErrorsCount(entity.getErrorsCount());
        simulation.setStartedAt(entity.getStartedAt());
        simulation.setFinishedAt(entity.getFinishedAt());
        simulation.setStatus(SessionStatus.valueOf(entity.getStatus()));
        return simulation;
    }
}