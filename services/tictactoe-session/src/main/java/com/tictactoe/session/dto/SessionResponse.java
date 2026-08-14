package com.tictactoe.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;

import java.util.List;

public record SessionResponse(String sessionId, GameState status, List<String> board, List<MoveRecord> moves,
                               Symbol winner, String errorCode, String errorMessage,
                               @JsonInclude(JsonInclude.Include.NON_NULL) String ownerToken) {
}
