package com.tictactoe.session.exception;

import com.tictactoe.common.error.ConflictException;
import com.tictactoe.common.error.ErrorCode;

public class SimulationAlreadyRunningException extends ConflictException {

    public SimulationAlreadyRunningException(String sessionId) {
        super(ErrorCode.SIMULATION_ALREADY_RUNNING, "A simulation is already running for session: " + sessionId);
    }
}
