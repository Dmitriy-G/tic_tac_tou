package com.tictactoe.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {
}