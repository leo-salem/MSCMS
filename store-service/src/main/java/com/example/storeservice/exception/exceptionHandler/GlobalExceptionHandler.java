package com.example.storeservice.exception.exceptionHandler;

import com.example.storeservice.exception.customException.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, WebRequest req) {
        log.warn(ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req, null);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidOperationException ex, WebRequest req) {
        log.warn(ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), req, null);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> unauthorized(UnauthorizedAccessException ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), req, null);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> noStock(InsufficientStockException ex, WebRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Stock", ex.getMessage(), req, null);
    }

    @ExceptionHandler(WalletOperationException.class)
    public ResponseEntity<ErrorResponse> wallet(WalletOperationException ex, WebRequest req) {
        log.warn("Wallet op failed: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Wallet Operation Failed", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> denied(AccessDeniedException ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String f = ((FieldError) err).getField();
            errors.put(f, err.getDefaultMessage());
        });
        return build(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields have invalid values", req, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed JSON request body", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParam(MissingServletRequestParameterException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Required parameter '" + ex.getParameterName() + "' is missing", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex, WebRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Parameter '" + ex.getName() + "' has invalid type", req, null);
    }

    @ExceptionHandler(StoreException.class)
    public ResponseEntity<ErrorResponse> store(StoreException ex, WebRequest req) {
        log.error("Store error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Store Error", ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unhandled(Exception ex, WebRequest req) {
        log.error("Unhandled error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus s, String error, String msg, WebRequest req, Map<String,String> ve) {
        return ResponseEntity.status(s).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(s.value()).error(error).message(msg)
                .path(req.getDescription(false).replace("uri=", ""))
                .validationErrors(ve).build());
    }
}

@lombok.Data @lombok.Builder @lombok.AllArgsConstructor @lombok.NoArgsConstructor
class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
}
