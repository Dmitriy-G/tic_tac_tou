package com.tictactoe.session.repository;

import com.tictactoe.session.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {

    /**
     * Claims a session for simulation only if it is still {@code CREATED}. {@code @Transactional}
     * here is a Spring Data requirement for {@code @Modifying} queries, not application-level
     * locking — the statement's own row lock lasts only for the duration of this single UPDATE.
     * Returns the row count so the caller can tell a won race (1) from a lost one (0) without
     * exception-driven control flow, the same compare-and-swap idiom used for the engine's board
     * (see {@code docs/adr/0003-optimistic-concurrency-on-moves.md}).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE SessionEntity s
               SET s.status = com.tictactoe.common.domain.GameState.IN_PROGRESS,
                   s.startedAt = :startedAt
             WHERE s.id = :id
               AND s.status = com.tictactoe.common.domain.GameState.CREATED
            """)
    int claimForSimulation(@Param("id") UUID id, @Param("startedAt") Instant startedAt);
}