package com.tictactoe.session.domain;

import com.tictactoe.common.domain.StepStatus;

import java.util.List;

public record SessionEvent(String sessionId, List<String> board, StepStatus stepStatus, String winner) {

}