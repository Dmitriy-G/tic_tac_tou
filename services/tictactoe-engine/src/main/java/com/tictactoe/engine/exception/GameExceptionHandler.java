package com.tictactoe.engine.exception;

import com.tictactoe.common.error.BaseGlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * No engine-specific overrides today: {@link GameNotFoundException} and the
 * {@code INVALID_GAME_ID} case both extend {@link com.tictactoe.common.error.BaseException} and
 * are handled by the inherited {@code handleBaseException}, and malformed JSON bodies are
 * handled by the inherited {@code handleMalformedBody}.
 */
@RestControllerAdvice
public class GameExceptionHandler extends BaseGlobalExceptionHandler {
}
