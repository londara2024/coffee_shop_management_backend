package com.aura.coffeeshop.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handling: every error leaves the API as an RFC 7807 {@code application/problem+json} body
 * with {@code type, title, status, detail, instance} plus a {@code timestamp} (and {@code errors} for validation).
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} covers all built-in Spring MVC exceptions (unknown route 404,
 * wrong method 405, unsupported media type 415, type mismatch 400, ...). The handlers below add the application's
 * own exceptions and a catch-all for anything unexpected.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ------------------------------------------------------------------ application exceptions

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(ex.getStatus(), "Business Rule Violation", ex.getMessage());
    }

    /** {@code @Min}/{@code @Max}/... on query & path parameters of {@code @Validated} controllers. */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.putIfAbsent(leafName(violation.getPropertyPath()), violation.getMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more parameters are invalid");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Data Conflict",
                "The request conflicts with existing data (duplicate value or record still referenced)");
    }

    /** Last resort: log the real cause, but never expose internals (messages, SQL, stack traces) to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    // ------------------------------------------------------------------ Spring MVC exceptions (customised)

    /** {@code @Valid @RequestBody} failures: one message per field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more fields are invalid");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Built-in method validation (parameters validated without a class-level {@code @Validated}). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = ex.getAllValidationResults().stream()
                .collect(Collectors.toMap(
                        result -> result.getMethodParameter().getParameterName(),
                        result -> result.getResolvableErrors().stream()
                                .map(e -> e.getDefaultMessage()).collect(Collectors.joining("; ")),
                        (a, b) -> a, LinkedHashMap::new));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more parameters are invalid");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Unparseable JSON: explain what is wrong without echoing Jackson's internal class names. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String detail;
        Throwable cause = ex.getCause();
        if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining(".")).replace(".[", "[");
            // Our own enum/value parsers throw IllegalArgumentException with a client-safe message.
            detail = ex.getMostSpecificCause() instanceof IllegalArgumentException iae
                    ? "Invalid value for '" + field + "': " + iae.getMessage()
                    : "Invalid value for '" + field + "'";
        } else if (cause instanceof JsonProcessingException) {
            detail = "Malformed JSON request body";
        } else {
            detail = "Request body is missing or unreadable";
        }
        return handleExceptionInternal(ex, problem(HttpStatus.BAD_REQUEST, "Malformed Request Body", detail),
                headers, status, request);
    }

    /**
     * Every response produced by the base class (404/405/415/...) passes through here with its final body
     * (Spring only builds the ProblemDetail for those just before this call): add the timestamp.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey("timestamp"))) {
            problem.setProperty("timestamp", Instant.now());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    // ------------------------------------------------------------------ helpers

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private static String leafName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name == null ? path.toString() : name;
    }
}
