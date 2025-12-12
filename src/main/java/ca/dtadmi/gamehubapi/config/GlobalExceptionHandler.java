package ca.dtadmi.gamehubapi.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
        List<Map<String, String>> violations = ex.getConstraintViolations().stream()
                .map(this::toViolation)
                .collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", violations);
    }

    private Map<String, String> toViolation(ConstraintViolation<?> v) {
        return Map.of(
                "property", v.getPropertyPath() == null ? "" : v.getPropertyPath().toString(),
                "message", v.getMessage() == null ? "invalid" : v.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", "Malformed JSON request", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "invalid_argument", ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Access is denied", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, Object details) {
        ApiError payload = new ApiError(code, message, details, Instant.now().toString());
        return ResponseEntity.status(status).body(payload);
    }

    record ApiError(String code, String message, Object details, String timestamp) {
    }
}
