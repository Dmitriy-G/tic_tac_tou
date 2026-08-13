package com.tictactoe.common.error;

import org.springframework.http.HttpStatus;

/**
 * A stable, wire-visible error code paired with the HTTP status it maps to.
 * The {@code code} string is part of the API contract; the enum constant name is not.
 */
public enum ErrorCode {

    // generic
    VALIDATION_ERROR("VALIDATION_ERROR", 400),
    MALFORMED_REQUEST("MALFORMED_REQUEST", 400),
    NOT_FOUND("NOT_FOUND", 404),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", 405),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", 415),
    CONFLICT("CONFLICT", 409),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", 429),
    INTERNAL_ERROR("INTERNAL_ERROR", 500),

    // engine
    GAME_NOT_FOUND("GAME_NOT_FOUND", 404),
    GAME_ALREADY_EXISTS("GAME_ALREADY_EXISTS", 409),
    INVALID_GAME_ID("INVALID_GAME_ID", 400),

    // session
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", 404),
    INVALID_SESSION_ID("INVALID_SESSION_ID", 400),
    SIMULATION_ALREADY_RUNNING("SIMULATION_ALREADY_RUNNING", 409),
    SESSION_ALREADY_COMPLETED("SESSION_ALREADY_COMPLETED", 409),
    SIMULATION_LIMIT_REACHED("SIMULATION_LIMIT_REACHED", 429),

    // dependency
    ENGINE_UNAVAILABLE("ENGINE_UNAVAILABLE", 503),
    ENGINE_STATE_LOST("ENGINE_STATE_LOST", 502),
    ENGINE_CONTRACT_VIOLATION("ENGINE_CONTRACT_VIOLATION", 500),
    ENGINE_BAD_RESPONSE("ENGINE_BAD_RESPONSE", 502),
    SIMULATION_TIMEOUT("SIMULATION_TIMEOUT", 500),
    DATABASE_ERROR("DATABASE_ERROR", 503);

    private final String code;
    private final int httpStatus;

    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public HttpStatus toHttpStatus() {
        return HttpStatus.valueOf(httpStatus);
    }
}