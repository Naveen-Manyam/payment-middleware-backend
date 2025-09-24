package com.phonepe.payment.exception;

import com.phonepe.payment.util.StructuredLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * Service for tracking and persisting exceptions with comprehensive logging.
 *
 * <p>This service provides centralized exception tracking functionality that
 * captures, logs, and persists exception information to the database for
 * monitoring and debugging purposes. It supports both simple exception logging
 * and contextual exception tracking with business operation details.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Automatic exception persistence to database</li>
 *   <li>Structured logging with unique exception IDs</li>
 *   <li>Context-aware exception tracking with transaction IDs</li>
 *   <li>Performance metrics for exception handling operations</li>
 *   <li>Fallback handling for persistence failures</li>
 *   <li>Stack trace capture and storage</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see StructuredLogger
 * @see TrackExceptionRepository
 */
@Service
@Slf4j
public class TrackExceptionService {

    @Autowired
    private TrackExceptionRepository trackExceptionRepository;

    public void logException(Exception ex) {
        long startTime = System.currentTimeMillis();
        String exceptionId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_START")
                .field("exceptionId", exceptionId)
                .field("exceptionType", ex.getClass().getSimpleName())
                .field("exceptionMessage", ex.getMessage())
                .timestamp()
                .debug("Starting exception tracking and persistence");

            ExceptionTrackResponse exceptionLog = ExceptionTrackResponse.builder()
                    .message(ex.getMessage())
                    .exception(getStackTraceAsString(ex))
                    .createdAt(LocalDateTime.now())
                    .build();

            trackExceptionRepository.save(exceptionLog);

            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_SUCCESS")
                .field("exceptionId", exceptionId)
                .field("exceptionType", ex.getClass().getSimpleName())
                .field("persistedToDatabase", true)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Exception tracked and persisted successfully");

        } catch (Exception persistenceException) {
            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_ERROR")
                .field("exceptionId", exceptionId)
                .field("originalExceptionType", ex.getClass().getSimpleName())
                .field("persistenceError", persistenceException.getMessage())
                .error(persistenceException)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Failed to persist exception to database", persistenceException);
        }
    }

    public void logExceptionWithContext(Exception ex, String operation, String transactionId, Object context) {
        long startTime = System.currentTimeMillis();
        String exceptionId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_WITH_CONTEXT_START")
                .field("exceptionId", exceptionId)
                .field("businessOperation", operation)
                .transactionId(transactionId)
                .field("exceptionType", ex.getClass().getSimpleName())
                .field("context", context)
                .timestamp()
                .debug("Starting exception tracking with business context");

            ExceptionTrackResponse exceptionLog = ExceptionTrackResponse.builder()
                    .message(ex.getMessage())
                    .exception(getStackTraceAsString(ex))
                    .createdAt(LocalDateTime.now())
                    .build();

            trackExceptionRepository.save(exceptionLog);

            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_WITH_CONTEXT_SUCCESS")
                .field("exceptionId", exceptionId)
                .field("businessOperation", operation)
                .transactionId(transactionId)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Exception with context tracked and persisted successfully");

        } catch (Exception persistenceException) {
            StructuredLogger.forLogger(log)
                .operation("EXCEPTION_TRACKING_WITH_CONTEXT_ERROR")
                .field("exceptionId", exceptionId)
                .field("businessOperation", operation)
                .transactionId(transactionId)
                .error(persistenceException)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Failed to persist exception with context to database", persistenceException);
        }
    }

    private String getStackTraceAsString(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public void logPerformanceMetric(String operation, long executionTimeMs, boolean isSlowOperation) {
        StructuredLogger.Patterns.logPerformanceMetric(log, operation, "TrackExceptionService",
            executionTimeMs, isSlowOperation);
    }
}
