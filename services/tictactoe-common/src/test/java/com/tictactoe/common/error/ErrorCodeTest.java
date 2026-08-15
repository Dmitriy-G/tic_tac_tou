package com.tictactoe.common.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The most direct evidence of "robust error handling": every {@link ErrorCode} pairs a stable
 * wire code with the HTTP status {@link BaseGlobalExceptionHandler} actually returns for it.
 */
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyErrorCodeHasANonBlankWireCodeAndAValidHttpStatus(ErrorCode errorCode) {
        assertThat(errorCode.getCode()).isNotBlank();
        assertThat(errorCode.getHttpStatus()).isBetween(400, 599);
        assertThat(errorCode.toHttpStatus().value()).isEqualTo(errorCode.getHttpStatus());
    }

    @Test
    void everyWireCodeIsUnique() {
        long distinctCodes = java.util.Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).distinct().count();
        assertThat(distinctCodes).isEqualTo(ErrorCode.values().length);
    }

    @Test
    void statusesMatchTheDocumentedTable() {
        assertThat(ErrorCode.VALIDATION_ERROR.getHttpStatus()).isEqualTo(400);
        assertThat(ErrorCode.MALFORMED_REQUEST.getHttpStatus()).isEqualTo(400);
        assertThat(ErrorCode.UNAUTHORIZED.getHttpStatus()).isEqualTo(401);
        assertThat(ErrorCode.NOT_SESSION_OWNER.getHttpStatus()).isEqualTo(403);
        assertThat(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus()).isEqualTo(405);
        assertThat(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getHttpStatus()).isEqualTo(415);
        assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus()).isEqualTo(500);
        assertThat(ErrorCode.GAME_NOT_FOUND.getHttpStatus()).isEqualTo(404);
        assertThat(ErrorCode.INVALID_GAME_ID.getHttpStatus()).isEqualTo(400);
        assertThat(ErrorCode.SESSION_NOT_FOUND.getHttpStatus()).isEqualTo(404);
        assertThat(ErrorCode.INVALID_SESSION_ID.getHttpStatus()).isEqualTo(400);
        assertThat(ErrorCode.SIMULATION_ALREADY_RUNNING.getHttpStatus()).isEqualTo(409);
        assertThat(ErrorCode.SESSION_ALREADY_COMPLETED.getHttpStatus()).isEqualTo(409);
        assertThat(ErrorCode.ENGINE_UNAVAILABLE.getHttpStatus()).isEqualTo(503);
        assertThat(ErrorCode.ENGINE_STATE_LOST.getHttpStatus()).isEqualTo(502);
        assertThat(ErrorCode.ENGINE_CONTRACT_VIOLATION.getHttpStatus()).isEqualTo(500);
        assertThat(ErrorCode.ENGINE_BAD_RESPONSE.getHttpStatus()).isEqualTo(502);
        assertThat(ErrorCode.DATABASE_ERROR.getHttpStatus()).isEqualTo(503);
    }
}
