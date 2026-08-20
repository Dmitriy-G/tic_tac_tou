package com.tictactoe.engine.repository;

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
 * {@link GameStore#compareAndSave(Game, List, com.tictactoe.common.domain.GameState)} writes the
 * new board only if the stored board still equals the one the caller loaded, so two overlapping
 * load-modify-save cycles on the same game can no longer clobber each other: the loser's write is
 * rejected and {@link GameService#applyMove} re-validates against the winner's board. No
 * serialisation exists or is needed — these tests exercise the real beans against the H2 test
 * database to prove the property holds. A {@link CountDownLatch} releases every thread together;
 * no {@code Thread.sleep}.
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
    void concurrentMovesToDifferentCellsStillProduceExactlyOneCorrectStep() throws Exception {
        // Turn order means at most one symbol may move at any instant, so nine threads
        // targeting nine *different* cells is still a nine-way race for a single slot.
        // Distinct cells rule out CELL_OCCUPIED as the reason only one wins — what rejects
        // the other eight is the turn having advanced.
        String gameId = UUID.randomUUID().toString();
        gameService.createGame(gameId);
        int threadCount = 9;

        List<StepStatus> results = runConcurrently(threadCount,
                cell -> gameService.applyMove(gameId, new MoveRequest(Symbol.X, cell)).stepStatus());

        assertThat(results)
                .as("exactly one of %d concurrent moves to distinct cells should win the single X turn", threadCount)
                .filteredOn(StepStatus.CORRECT_STEP::equals)
                .hasSize(1);
    }

    @Test
    void concurrentMovesLeaveExactlyOneSymbolOnTheBoard() throws Exception {
        // The lost-update assertion, stated correctly: after N concurrent attempts the
        // persisted board holds exactly one symbol. Guards against two writes both landing
        // and against the winning write being clobbered.
        String gameId = UUID.randomUUID().toString();
        gameService.createGame(gameId);
        int threadCount = 9;

        runConcurrently(threadCount,
                cell -> gameService.applyMove(gameId, new MoveRequest(Symbol.X, cell)).stepStatus());

        List<String> finalBoard = gameStore.load(gameId).board();
        long symbolCount = finalBoard.stream().filter(cell -> !BoardUtils.EMPTY_CELL.equals(cell)).count();
        assertThat(symbolCount)
                .as("exactly one symbol should be persisted after %d concurrent attempts, no lost update and no clobber", threadCount)
                .isEqualTo(1);
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
