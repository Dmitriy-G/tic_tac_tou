package com.tictactoe.session.exception;

public class SimulationAlreadyRunningException extends RuntimeException {

    public SimulationAlreadyRunningException(String sessionId) {
        super("Session " + sessionId + " cannot be simulated again in its current state");
    }
}