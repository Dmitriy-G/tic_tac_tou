package com.tictactoe.common.dto;

import com.tictactoe.common.domain.Symbol;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(@NotNull Symbol symbol, @Min(0) @Max(8) int position) {
}
