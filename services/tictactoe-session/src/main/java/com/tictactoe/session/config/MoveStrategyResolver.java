package com.tictactoe.session.config;

import com.tictactoe.session.strategy.MoveStrategy;
import com.tictactoe.session.strategy.RandomMoveStrategy;
import com.tictactoe.session.strategy.RuleBasedMoveStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Component
public class MoveStrategyResolver {

    private final Map<String, MoveStrategy> strategiesBySymbol;

    public MoveStrategyResolver(Random random,
                                 @Value("${simulation.strategy.x:SIMPLE}") String xStrategyType,
                                 @Value("${simulation.strategy.o:SIMPLE}") String oStrategyType) {
        this.strategiesBySymbol = Map.of(
                "X", createStrategy(xStrategyType, random),
                "O", createStrategy(oStrategyType, random));
    }

    public MoveStrategy resolve(String symbol) {
        MoveStrategy strategy = strategiesBySymbol.get(symbol);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown symbol: " + symbol);
        }
        return strategy;
    }

    private static MoveStrategy createStrategy(String type, Random random) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "SIMPLE" -> new RandomMoveStrategy(random);
            case "ADVANCED" -> new RuleBasedMoveStrategy();
            default -> throw new IllegalArgumentException("Unknown simulation strategy type: " + type);
        };
    }
}