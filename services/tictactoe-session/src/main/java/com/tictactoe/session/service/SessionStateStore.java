package com.tictactoe.session.service;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveResponse;
import com.tictactoe.common.error.ErrorCode;
import com.tictactoe.session.dto.MoveRecord;
import com.tictactoe.session.util.BoardUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The session's board and move history, live for as long as the process runs. Deliberately not
 * persisted — see the Storage section in the root CLAUDE.md — so it is mutated only through
 * {@link Map#compute}, never a plain get-then-put, to keep a concurrent read (GET
 * /sessions/{id}) and a concurrent write (the next move landing) from tearing. Both the SSE
 * stream and GET /sessions/{id} are populated from here, which is what makes the stream an
 * optimisation rather than a dependency: either can rebuild the same state.
 */
@Component
public class SessionStateStore {

    private final Map<String, LiveState> state = new ConcurrentHashMap<>();

    void initialize(String sessionId) {
        state.put(sessionId, LiveState.initial());
    }

    void markRunning(String sessionId) {
        state.compute(sessionId, (id, current) -> LiveState.initial().withStatus(GameState.IN_PROGRESS));
    }

    void recordMove(String sessionId, int moveNumber, Symbol symbol, int position, MoveResponse response) {
        state.compute(sessionId, (id, current) -> {
            LiveState base = current != null ? current : LiveState.initial();
            List<MoveRecord> moves = new ArrayList<>(base.moves());
            moves.add(new MoveRecord(moveNumber, symbol, position, response.stepStatus()));
            return new LiveState(response.gameState(), List.copyOf(response.board()), List.copyOf(moves),
                    response.winner(), null, null);
        });
    }

    void recordFailure(String sessionId, ErrorCode errorCode, String errorMessage) {
        state.compute(sessionId, (id, current) -> {
            LiveState base = current != null ? current : LiveState.initial();
            return new LiveState(GameState.FAILED, base.board(), base.moves(), base.winner(),
                    errorCode.getCode(), errorMessage);
        });
    }

    LiveState get(String sessionId) {
        return state.getOrDefault(sessionId, LiveState.initial());
    }

    record LiveState(GameState status, List<String> board, List<MoveRecord> moves, Symbol winner,
                      String errorCode, String errorMessage) {

        static LiveState initial() {
            return new LiveState(GameState.CREATED, Collections.nCopies(9, BoardUtils.EMPTY_CELL),
                    List.of(), null, null, null);
        }

        LiveState withStatus(GameState newStatus) {
            return new LiveState(newStatus, board, moves, winner, errorCode, errorMessage);
        }
    }
}
