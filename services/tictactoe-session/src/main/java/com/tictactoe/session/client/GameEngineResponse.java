package com.tictactoe.session.client;

import com.tictactoe.session.domain.SimulationStatus;

import java.util.List;

public record GameEngineResponse(List<String> board, SimulationStatus simulationStatus, SimulationStatus stepStatus) {
}