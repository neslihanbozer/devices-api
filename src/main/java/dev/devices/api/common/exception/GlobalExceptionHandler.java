package dev.devices.api.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidNullException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Returns every error as an RFC 9457 Problem Detail
 * ({@code application/problem+json}) so clients get one consistent format.
 * Framework exceptions (malformed JSON, type mismatches, ...) are handled
 * by {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DeviceNotFoundException.class)
    public ProblemDetail handleDeviceNotFound(DeviceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Device not found", ex.getMessage());
    }

    @ExceptionHandler(DeviceConflictException.class)
    public ProblemDetail handleDeviceConflict(DeviceConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Business rule violation", ex.getMessage());
    }

    /**
     * Another request modified or deleted the device after it was read.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Concurrent modification detected: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Concurrent modification",
                "The device was modified by another request. Reload it and try again.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "One or more fields are invalid.");
        body.setProperty("errors", errors);

        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Unreadable request body. When the problem is tied to a specific field
     * (explicit null, invalid enum value, unknown or read-only field), the
     * response names that field instead of a generic "Failed to read request".
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        JacksonException jsonError = findCause(ex, JacksonException.class);
        String field = jsonError == null ? null : fieldPath(jsonError);

        ProblemDetail body;
        if (field != null) {
            body = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                    "One or more fields are invalid.");
            body.setProperty("errors", Map.of(field, fieldMessage(jsonError, ex)));
        } else {
            body = problem(HttpStatus.BAD_REQUEST, "Malformed request",
                    "The request body is missing or is not valid JSON.");
        }

        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static String fieldPath(JacksonException ex) {
        String path = ex.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
        return path.isEmpty() ? null : path;
    }

    private static String fieldMessage(JacksonException jsonError, Throwable ex) {
        if (jsonError instanceof InvalidNullException) {
            return "must not be null";
        }
        if (jsonError instanceof InvalidFormatException format
                && format.getTargetType() != null
                && format.getTargetType().isEnum()) {
            return "must be one of " + Arrays.toString(format.getTargetType().getEnumConstants());
        }
        if (findCause(ex, IllegalArgumentException.class) != null) {
            // Thrown by PatchDeviceRequest for unknown or read-only fields (e.g. id, createdAt).
            return "unknown or read-only field";
        }
        return "has an invalid value";
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
        }
        return null;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
