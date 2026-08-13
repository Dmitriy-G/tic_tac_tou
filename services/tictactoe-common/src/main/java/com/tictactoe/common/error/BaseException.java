package com.tictactoe.common.error;

/**
 * Base type for every fault (see the rule-outcome vs. fault distinction in
 * {@code docs/adr/0003-error-channels.md}) raised by either service. Carries the {@link ErrorCode}
 * that {@code BaseGlobalExceptionHandler} maps to an HTTP status and wire code.
 */
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}