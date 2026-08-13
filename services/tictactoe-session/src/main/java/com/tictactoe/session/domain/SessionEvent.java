package com.tictactoe.session.domain;

import java.util.List;

public record SessionEvent(String sessionId, List<String> board, StepStatus stepStatus, String winner) {

}