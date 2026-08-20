package com.tictactoe.session.repository;

import com.tictactoe.session.dto.MoveRecord;
import com.tictactoe.session.entity.SessionMoveEntity;

public final class SessionMoveMapper {

    private SessionMoveMapper() {
    }

    public static MoveRecord toRecord(SessionMoveEntity entity) {
        return new MoveRecord(entity.getId().moveNumber(), entity.getSymbol(), entity.getPosition(), entity.getStepStatus());
    }
}