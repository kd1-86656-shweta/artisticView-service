package com.artisticview.api.support;

import com.artisticview.common.api.ApiError;
import com.artisticview.common.api.ApiResponse;
import com.artisticview.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(AppException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.fail(ApiError.of(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> details.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(new ApiError("VALIDATION_FAILED", "Please fix the highlighted fields", details)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestValueException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadInput(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.of("BAD_REQUEST", "Malformed request")));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleDenied(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ApiError.of("FORBIDDEN", "You do not have access to this")));
    }

    /** Unique-constraint races (e.g. two sign-ups with the same username at once). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException e) {
        log.info("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ApiError.of("CONFLICT", "This conflicts with existing data")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(ApiError.of("NOT_FOUND", "Not found")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ApiError.of("INTERNAL_ERROR", "Something went wrong. Please try again.")));
    }
}
