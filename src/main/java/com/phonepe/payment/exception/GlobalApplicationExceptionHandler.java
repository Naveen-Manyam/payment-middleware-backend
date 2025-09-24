package com.phonepe.payment.exception;

import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.util.StructuredLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Global exception handler for the entire PhonePe MiddleWare application.
 *
 * <p>This controller advice provides centralized exception handling for all
 * unhandled exceptions across the application. It serves as the fallback
 * exception handler with the lowest precedence, ensuring that any exceptions
 * not caught by module-specific handlers are properly handled and logged.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Handles common Spring framework exceptions (validation, HTTP errors)</li>
 *   <li>Provides standardized error responses with unique request IDs</li>
 *   <li>Comprehensive structured logging for all exceptions</li>
 *   <li>Security violation detection and logging</li>
 *   <li>Automatic exception persistence via TrackExceptionService</li>
 *   <li>User-friendly error messages for client applications</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see TrackExceptionService
 * @see FailureApiResponse
 */
@RestControllerAdvice
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalApplicationExceptionHandler {

    @Autowired
    private TrackExceptionService trackExceptionService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("VALIDATION_ERROR")
            .requestId(requestId)
            .field("exceptionType", "MethodArgumentNotValidException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("validationErrors", ex.getBindingResult().getAllErrors())
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Validation failed for request", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "VALIDATION_ERROR",
                    "Validation failed: " + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("MALFORMED_REQUEST")
            .requestId(requestId)
            .field("exceptionType", "HttpMessageNotReadableException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Malformed JSON request", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "MALFORMED_REQUEST",
                    "Invalid JSON format in request body", null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("METHOD_NOT_SUPPORTED")
            .requestId(requestId)
            .field("exceptionType", "HttpRequestMethodNotSupportedException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("supportedMethods", Arrays.toString(ex.getSupportedMethods()))
            .httpStatus(HttpStatus.METHOD_NOT_ALLOWED.value())
            .error(ex)
            .timestamp()
            .error("HTTP method not supported", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new FailureApiResponse<>(false, "METHOD_NOT_SUPPORTED",
                    "HTTP method " + request.getMethod() + " not supported for this endpoint", null));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("ENDPOINT_NOT_FOUND")
            .requestId(requestId)
            .field("exceptionType", "NoHandlerFoundException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .httpStatus(HttpStatus.NOT_FOUND.value())
            .error(ex)
            .timestamp()
            .error("Endpoint not found", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new FailureApiResponse<>(false, "ENDPOINT_NOT_FOUND",
                    "The requested endpoint was not found", null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("MISSING_PARAMETER")
            .requestId(requestId)
            .field("exceptionType", "MissingServletRequestParameterException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("missingParameter", ex.getParameterName())
            .field("parameterType", ex.getParameterType())
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Missing required parameter", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "MISSING_PARAMETER",
                    "Missing required parameter: " + ex.getParameterName(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("TYPE_MISMATCH")
            .requestId(requestId)
            .field("exceptionType", "MethodArgumentTypeMismatchException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("parameterName", ex.getName())
            .field("providedValue", ex.getValue())
            .field("requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown")
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Parameter type mismatch", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "TYPE_MISMATCH",
                    "Invalid parameter type for: " + ex.getName(), null));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("SECURITY_EXCEPTION")
            .requestId(requestId)
            .field("exceptionType", "SecurityException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("remoteAddr", request.getRemoteAddr())
            .field("userAgent", request.getHeader("User-Agent"))
            .httpStatus(HttpStatus.FORBIDDEN.value())
            .error(ex)
            .timestamp()
            .error("Security violation detected", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new FailureApiResponse<>(false, "SECURITY_VIOLATION",
                    "Access denied", null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("ILLEGAL_ARGUMENT")
            .requestId(requestId)
            .field("exceptionType", "IllegalArgumentException")
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .httpStatus(HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Invalid argument provided", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "INVALID_ARGUMENT",
                    ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided", null));
    }

    // Catch-all handler for any unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, HttpServletRequest request) {
        String requestId = StructuredLogger.generateRequestId();

        StructuredLogger.forLogger(log)
            .operation("UNHANDLED_EXCEPTION")
            .requestId(requestId)
            .field("exceptionType", ex.getClass().getSimpleName())
            .field("endpoint", request.getRequestURI())
            .field("method", request.getMethod())
            .field("timestamp", LocalDateTime.now().toString())
            .field("remoteAddr", request.getRemoteAddr())
            .field("userAgent", request.getHeader("User-Agent"))
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(ex)
            .timestamp()
            .error("Unhandled exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FailureApiResponse<>(false, "INTERNAL_SERVER_ERROR",
                    "An unexpected error occurred. Please contact support with request ID: " + requestId, null));
    }
}