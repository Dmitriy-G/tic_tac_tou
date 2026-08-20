package com.tictactoe.session.repository;

import com.tictactoe.session.entity.SessionMoveEntity;
import com.tictactoe.session.entity.SessionMoveId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionMoveJpaRepository extends JpaRepository<SessionMoveEntity, SessionMoveId> {

    @Query("SELECT m FROM SessionMoveEntity m WHERE m.id.sessionId = :sessionId ORDER BY m.id.moveNumber")
    List<SessionMoveEntity> findBySessionIdOrderByMoveNumber(@Param("sessionId") UUID sessionId);
}