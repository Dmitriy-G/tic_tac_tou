package com.tictactoe.engine.repository;

import com.tictactoe.common.domain.GameState;
import com.tictactoe.common.domain.StepStatus;
import com.tictactoe.common.domain.Symbol;
import com.tictactoe.common.dto.MoveRequest;
import com.tictactoe.engine.EngineApplication;
import com.tictactoe.engine.service.GameService;
import com.tictactoe.engine.util.BoardUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GameStore#save(Game, List, GameState)} writes the whole 9-character board on every call,
 * so two overlapping load-modify-save cycles on the same game clobber each other regardless of
 * which cells they touch, unless callers are serialised per game id. Nothing in {@link GameStore}
 * or {@link GameService} currently provides that serialisation — these tests exercise the real
 * beans against the H2 test database and prove (or disprove) the ADRs' concurrent-move-safety
 * claim. A {@link CountDownLatch} releases every thread together; no {@code Thread.sleep}.
 */
@SpringBootTest(classes = EngineApplication.class)
@ActiveProfiles("test")
class GameStoreConcurrencyTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameStore gameStore;

    @Test
    void concurrentMovesToTheSameCellProduceExactlyOneCorrectStep() throws Exception {
        String gameId = UUID.randomUUID().toString();
        gameService.createGame(gameId);
        int threadCount = 8;

        List<StepStatus> results = runConcurrently(threadCount,
                cell -> gameService.applyMove(gameId, new MoveRequest(Symbol.X, 4)).stepStatus());

        // Only "nobody else also succeeded" is pinned, not one specific rejection code:
        // MoveValidatorTest documents that a late reader of an occupied cell can surface as
        // OUT_OF_TURN rather than CELL_OCCUPIED once the winning move has advanced the turn.
        assertThat(results)
                .as("exactly one of %d concurrent moves to the same cell should win", threadCount)
                .filteredOn(StepStatus.CORRECT_STEP::equals)
                .hasSize(1);
    }

    @Test
    void concurrentMovesToDifferentCellsLoseNoMoves() throws Exception {
        String gameId = UUID.randomUUID().toString();
        gameStore.create(gameId, BoardUtils.emptyBoard());
        int threadCount = 9;

        runConcurrently(threadCount, cell -> {
            Game game = gameStore.load(gameId);
            List<String> board = new ArrayList<>(game.board());
            board.set(cell, Symbol.X.name());
            gameStore.save(game, board, GameState.IN_PROGRESS);
            return null;
        });

        List<String> finalBoard = gameStore.load(gameId).board();
        long symbolCount = finalBoard.stream().filter(cell -> !BoardUtils.EMPTY_CELL.equals(cell)).count();
        assertThat(symbolCount)
                .as("every one of %d concurrent single-cell writes should be reflected in the final board", threadCount)
                .isEqualTo(threadCount);
    }

    /**
     * Runs {@code task} once per thread (task index passed as {@code cell}), releasing every
     * thread together via a latch so the race is real rather than timing-dependent.
     */
    private static <T> List<T> runConcurrently(int threadCount, IntFunction<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                int cell = i;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.apply(cell);
                }));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdown();
        }
    }
}
