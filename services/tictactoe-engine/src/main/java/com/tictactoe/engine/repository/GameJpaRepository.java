package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.engine.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, UUID> {

    /**
     * Writes {@code newBoard}/{@code state} only if the stored board still equals
     * {@code expectedBoard}. {@code @Transactional} here is a Spring Data requirement for
     * {@code @Modifying} queries, not application-level locking — the statement's own row lock
     * lasts only for the duration of this single UPDATE. Returns the row count so the caller can
     * tell a won race (1) from a lost one (0) without exception-driven control flow.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE GameEntity g
               SET g.board = :newBoard, g.state = :state
             WHERE g.id = :id
               AND g.board = :expectedBoard
            """)
    int compareAndSwapBoard(@Param("id") UUID id,
                            @Param("expectedBoard") String expectedBoard,
                            @Param("newBoard") String newBoard,
                            @Param("state") GameState state);
}