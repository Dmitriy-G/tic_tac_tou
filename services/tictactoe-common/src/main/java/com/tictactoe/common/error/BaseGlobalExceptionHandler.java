package com.tictactoe.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

public abstract class BaseGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (code.getHttpStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("{} {} -> {} {}: {}", method, uri, code.getHttpStatus(), code.getCode(), e.getMessage(), e);
        } else {
            log.warn("{} {} -> {} {}: {}", method, uri, code.getHttpStatus(), code.getCode(), e.getMessage());
        }
        return respond(code, e.getMessage(), request, null);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e,
                                                                        HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorResponse.FieldError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        log.warn("{} {} -> 400 VALIDATION_ERROR: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, "Request parameters failed validation", request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                       HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        log.warn("{} {} -> 400 VALIDATION_ERROR: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, "Request body failed validation", request, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                     HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations().stream()
                .map(this::toFieldError)
                .toList();
        log.warn("{} {} -> 400 VALIDATION_ERROR: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, "Request failed validation", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedBody(HttpServletRequest request) {
        log.warn("{} {} -> 400 MALFORMED_REQUEST", request.getMethod(), request.getRequestURI());
        return respond(ErrorCode.MALFORMED_REQUEST, "Request body is malformed", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e,
                                                                  HttpServletRequest request) {
        log.warn("{} {} -> 400 VALIDATION_ERROR: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, e.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                              HttpServletRequest request) {
        log.warn("{} {} -> 400 VALIDATION_ERROR: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.VALIDATION_ERROR, e.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("{} {} -> 400 MALFORMED_REQUEST: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.MALFORMED_REQUEST, e.getMessage(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                    HttpServletRequest request) {
        log.warn("{} {} -> 405 METHOD_NOT_ALLOWED: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage(), request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                                       HttpServletRequest request) {
        log.warn("{} {} -> 415 UNSUPPORTED_MEDIA_TYPE: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), request, null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e, HttpServletRequest request) {
        log.error("{} {} -> 503 DATABASE_ERROR", request.getMethod(), request.getRequestURI(), e);
        return respond(ErrorCode.DATABASE_ERROR, "A database error occurred", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("{} {} -> 500 INTERNAL_ERROR", request.getMethod(), request.getRequestURI(), e);
        return respond(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, null);
    }

    private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
        return new ErrorResponse.FieldError(violation.getPropertyPath().toString(), violation.getMessage());
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, HttpServletRequest request,
                                                    List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = ErrorResponse.of(code, message, request.getRequestURI(), MDC.get("traceId"), fieldErrors);
        return ResponseEntity.status(code.toHttpStatus()).body(body);
    }

}