package com.expenseassistant.common.exception;

import com.expenseassistant.common.response.ErrorResponse;
import com.expenseassistant.common.response.ErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String AI_UNAVAILABLE = "AI_UNAVAILABLE";
    private static final String ACTION_EXPIRED = "ACTION_EXPIRED";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    private static final String GENERIC_INTERNAL_MESSAGE = "An unexpected error occurred";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return respond(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR,
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors.isEmpty() ? null : fieldErrors);
    }

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailed(
            ValidationFailedException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getFieldErrors();
        return respond(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR,
                ex.getMessage(),
                request.getRequestURI(),
                fieldErrors.isEmpty() ? null : fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respond(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR,
                "Malformed JSON body",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value for parameter '%s'".formatted(ex.getName());
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message, request.getRequestURI(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(PendingActionExpiredException.class)
    public ResponseEntity<ErrorResponse> handlePendingActionExpired(
            PendingActionExpiredException ex, HttpServletRequest request) {
        return respond(
                HttpStatus.GONE,
                ACTION_EXPIRED,
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAiUnavailable(
            AiUnavailableException ex, HttpServletRequest request) {
        if (ex.getCause() != null) {
            log.warn("AI assistant unavailable on {}: {}", request.getRequestURI(), ex.getCause().toString());
        } else {
            log.warn("AI assistant unavailable on {}", request.getRequestURI());
        }
        return respond(
                HttpStatus.SERVICE_UNAVAILABLE,
                AI_UNAVAILABLE,
                ex.getMessage(),
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR,
                GENERIC_INTERNAL_MESSAGE,
                request.getRequestURI(),
                null);
    }

    private FieldError toFieldError(org.springframework.validation.FieldError error) {
        return new FieldError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String error, String message, String path, List<FieldError> fieldErrors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, path, fieldErrors));
    }
}