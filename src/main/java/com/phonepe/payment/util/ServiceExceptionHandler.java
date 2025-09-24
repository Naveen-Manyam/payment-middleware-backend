package com.phonepe.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Centralized exception handling utility for service layer operations.
 *
 * <p>This utility provides comprehensive exception analysis, categorization,
 * and structured logging for all types of exceptions that can occur during
 * service operations. It includes specialized handlers for PhonePe API errors,
 * WebClient exceptions, database errors, and validation failures.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Automatic exception categorization and error code assignment</li>
 *   <li>User-friendly error message generation</li>
 *   <li>Structured exception logging with context</li>
 *   <li>PhonePe-specific API error analysis</li>
 *   <li>Retry strategy recommendations based on exception type</li>
 *   <li>Security violation detection and logging</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see ExceptionAnalysis
 * @see StructuredLogger
 */
@Slf4j
public class ServiceExceptionHandler {

    public static class ExceptionAnalysis {
        private final String errorCode;
        private final String userMessage;
        private final String logOperation;
        private final Exception originalException;

        public ExceptionAnalysis(String errorCode, String userMessage, String logOperation, Exception originalException) {
            this.errorCode = errorCode;
            this.userMessage = userMessage;
            this.logOperation = logOperation;
            this.originalException = originalException;
        }

        public String getErrorCode() { return errorCode; }
        public String getUserMessage() { return userMessage; }
        public String getLogOperation() { return logOperation; }
        public Exception getOriginalException() { return originalException; }
    }

    public static ExceptionAnalysis analyzeException(Exception e, String baseOperation) {
        if (e instanceof JsonProcessingException) {
            return new ExceptionAnalysis(
                "JSON_PROCESSING_ERROR",
                "Invalid request format: " + e.getMessage(),
                baseOperation + "_JSON_ERROR",
                e
            );
        }

        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            return new ExceptionAnalysis(
                "EXTERNAL_API_ERROR",
                "External service error (HTTP " + wcre.getStatusCode() + "): " + wcre.getMessage(),
                baseOperation + "_API_RESPONSE_ERROR",
                e
            );
        }

        if (e instanceof WebClientRequestException) {
            return new ExceptionAnalysis(
                "EXTERNAL_API_REQUEST_ERROR",
                "Failed to connect to external service: " + e.getMessage(),
                baseOperation + "_API_REQUEST_ERROR",
                e
            );
        }

        if (e instanceof WebClientException) {
            return new ExceptionAnalysis(
                "EXTERNAL_API_CONNECTION_ERROR",
                "External service unavailable: " + e.getMessage(),
                baseOperation + "_WEBCLIENT_ERROR",
                e
            );
        }

        if (e instanceof ConnectException) {
            return new ExceptionAnalysis(
                "CONNECTION_REFUSED",
                "Unable to connect to external service",
                baseOperation + "_CONNECTION_ERROR",
                e
            );
        }

        if (e instanceof SocketTimeoutException || e instanceof TimeoutException) {
            return new ExceptionAnalysis(
                "TIMEOUT_ERROR",
                "Operation timed out: " + e.getMessage(),
                baseOperation + "_TIMEOUT_ERROR",
                e
            );
        }

        if (e instanceof DataAccessException) {
            return new ExceptionAnalysis(
                "DATABASE_ERROR",
                "Database operation failed: " + e.getMessage(),
                baseOperation + "_DATABASE_ERROR",
                e
            );
        }

        if (e instanceof IllegalArgumentException) {
            return new ExceptionAnalysis(
                "INVALID_ARGUMENT",
                "Invalid input: " + e.getMessage(),
                baseOperation + "_VALIDATION_ERROR",
                e
            );
        }

        if (e instanceof SecurityException) {
            return new ExceptionAnalysis(
                "SECURITY_VIOLATION",
                "Security validation failed",
                baseOperation + "_SECURITY_ERROR",
                e
            );
        }

        // Default case for unexpected exceptions
        return new ExceptionAnalysis(
            "UNEXPECTED_ERROR",
            "Unexpected error occurred: " + e.getMessage(),
            baseOperation + "_UNEXPECTED_ERROR",
            e
        );
    }

    public static void logException(Logger logger, ExceptionAnalysis analysis, String transactionId,
                                  long startTime, Object additionalContext) {
        StructuredLogger.LogBuilder logBuilder = StructuredLogger.forLogger(logger)
            .operation(analysis.getLogOperation())
            .transactionId(transactionId)
            .field("errorCode", analysis.getErrorCode())
            .error(analysis.getOriginalException())
            .responseTime(System.currentTimeMillis() - startTime)
            .timestamp();

        if (additionalContext != null) {
            logBuilder.field("context", additionalContext);
        }

        // Log at appropriate level based on exception type
        if (analysis.getErrorCode().contains("VALIDATION") ||
            analysis.getErrorCode().contains("ARGUMENT") ||
            analysis.getErrorCode().contains("JSON")) {
            logBuilder.warn(analysis.getUserMessage());
        } else if (analysis.getErrorCode().contains("SECURITY")) {
            logBuilder.error("Security violation detected: " + analysis.getUserMessage());
        } else {
            logBuilder.error(analysis.getUserMessage());
        }
    }

    public static class PhonePeApiExceptionAnalyzer {

        public static ExceptionAnalysis analyzePhonePeResponse(String responseBody, int httpStatus, String operation) {
            String errorCode;
            String userMessage;

            switch (httpStatus) {
                case 400:
                    errorCode = "PHONEPE_BAD_REQUEST";
                    userMessage = "Invalid request sent to PhonePe";
                    break;
                case 401:
                    errorCode = "PHONEPE_UNAUTHORIZED";
                    userMessage = "Authentication failed with PhonePe";
                    break;
                case 403:
                    errorCode = "PHONEPE_FORBIDDEN";
                    userMessage = "Access denied by PhonePe";
                    break;
                case 404:
                    errorCode = "PHONEPE_NOT_FOUND";
                    userMessage = "Resource not found on PhonePe";
                    break;
                case 429:
                    errorCode = "PHONEPE_RATE_LIMITED";
                    userMessage = "Rate limit exceeded on PhonePe";
                    break;
                case 500:
                    errorCode = "PHONEPE_INTERNAL_ERROR";
                    userMessage = "PhonePe internal server error";
                    break;
                case 502:
                case 503:
                case 504:
                    errorCode = "PHONEPE_SERVICE_UNAVAILABLE";
                    userMessage = "PhonePe service temporarily unavailable";
                    break;
                default:
                    errorCode = "PHONEPE_UNKNOWN_ERROR";
                    userMessage = "Unknown error from PhonePe (HTTP " + httpStatus + ")";
            }

            return new ExceptionAnalysis(
                errorCode,
                userMessage,
                operation + "_PHONEPE_API_ERROR",
                new RuntimeException("PhonePe API Error: " + responseBody)
            );
        }
    }

    public static class RetryableExceptionChecker {

        public static boolean isRetryable(Exception e) {
            return e instanceof SocketTimeoutException ||
                   e instanceof TimeoutException ||
                   e instanceof ConnectException ||
                   (e instanceof WebClientRequestException) ||
                   (e instanceof WebClientResponseException &&
                    isRetryableHttpStatus(((WebClientResponseException) e).getStatusCode().value()));
        }

        private static boolean isRetryableHttpStatus(int status) {
            return status == 429 || // Rate Limited
                   status == 502 || // Bad Gateway
                   status == 503 || // Service Unavailable
                   status == 504;   // Gateway Timeout
        }

        public static long getRetryDelay(int attemptNumber) {
            // Exponential backoff: 1s, 2s, 4s, 8s, 16s
            return Math.min(1000L * (1L << attemptNumber), 16000L);
        }
    }
}