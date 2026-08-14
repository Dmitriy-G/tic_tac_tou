package com.tictactoe.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String code, String message,
                             String path, String traceId, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(ErrorCode code, String message, String path, String traceId) {
        return new ErrorResponse(Instant.now(), code.getHttpStatus(), code.getCode(), message, path, traceId, null);
    }

    public static ErrorResponse of(ErrorCode code, String message, String path, String traceId,
                                    List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), code.getHttpStatus(), code.getCode(), message, path, traceId,
                fieldErrors);
    }
}