package com.tictactoe.session.exception;

import com.tictactoe.common.error.BaseGlobalExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * No session-specific overrides today: {@link SessionNotFoundException} and
 * {@link SimulationAlreadyRunningException} both extend
 * {@link com.tictactoe.common.error.BaseException} and are handled by the inherited
 * {@code handleBaseException}.
 */
@RestControllerAdvice
public class SessionExceptionHandler extends BaseGlobalExceptionHandler {
}
