package com.phonepe.payment.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Structured logging utility for PhonePe MiddleWare application.
 *
 * <p>This utility provides a builder pattern approach to creating structured JSON logs
 * with consistent formatting across the application. It includes support for MDC
 * (Mapped Diagnostic Context) for thread-local context preservation and predefined
 * logging patterns for common operations like API requests, database operations,
 * and business events.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Thread-safe structured logging with JSON output format</li>
 *   <li>Builder pattern for flexible log entry construction</li>
 *   <li>MDC integration for correlation tracking</li>
 *   <li>Predefined patterns for common logging scenarios</li>
 *   <li>Performance metrics and error tracking</li>
 *   <li>Security event logging capabilities</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see org.slf4j.MDC
 * @see com.fasterxml.jackson.databind.ObjectMapper
 */
public class StructuredLogger {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class LogBuilder {
        private final Logger logger;
        private final Map<String, Object> logData = new HashMap<>();

        public LogBuilder(Logger logger) {
            this.logger = logger;
        }

        public LogBuilder transactionId(String transactionId) {
            logData.put("transactionId", transactionId);
            MDC.put("transactionId", transactionId);
            return this;
        }

        public LogBuilder merchantId(String merchantId) {
            logData.put("merchantId", merchantId);
            MDC.put("merchantId", merchantId);
            return this;
        }

        public LogBuilder amount(Object amount) {
            logData.put("amount", amount);
            return this;
        }

        public LogBuilder status(String status) {
            logData.put("status", status);
            return this;
        }

        public LogBuilder operation(String operation) {
            logData.put("operation", operation);
            MDC.put("operation", operation);
            return this;
        }

        public LogBuilder phonepeTransactionId(String phonepeTransactionId) {
            logData.put("phonepeTransactionId", phonepeTransactionId);
            return this;
        }

        public LogBuilder httpMethod(String httpMethod) {
            logData.put("httpMethod", httpMethod);
            return this;
        }

        public LogBuilder endpoint(String endpoint) {
            logData.put("endpoint", endpoint);
            return this;
        }

        public LogBuilder responseTime(long responseTimeMs) {
            logData.put("responseTimeMs", responseTimeMs);
            return this;
        }

        public LogBuilder httpStatus(int httpStatus) {
            logData.put("httpStatus", httpStatus);
            return this;
        }

        public LogBuilder requestId(String requestId) {
            logData.put("requestId", requestId);
            MDC.put("requestId", requestId);
            return this;
        }

        public LogBuilder error(Throwable throwable) {
            logData.put("errorMessage", throwable.getMessage());
            logData.put("errorClass", throwable.getClass().getSimpleName());
            if (throwable.getCause() != null) {
                logData.put("rootCause", throwable.getCause().getMessage());
            }
            return this;
        }

        public LogBuilder field(String key, Object value) {
            logData.put(key, value);
            return this;
        }

        public LogBuilder timestamp() {
            logData.put("timestamp", Instant.now().toString());
            return this;
        }

        private String formatMessage(String message) {
            try {
                Map<String, Object> fullLog = new HashMap<>();
                fullLog.put("message", message);
                fullLog.putAll(logData);
                return objectMapper.writeValueAsString(fullLog);
            } catch (JsonProcessingException e) {
                // Fallback to simple message if JSON serialization fails
                return message + " [JSON_SERIALIZATION_ERROR: " + e.getMessage() + "]";
            }
        }

        public void info(String message) {
            logger.info(formatMessage(message));
        }

        public void debug(String message) {
            logger.debug(formatMessage(message));
        }

        public void warn(String message) {
            logger.warn(formatMessage(message));
        }

        public void error(String message) {
            logger.error(formatMessage(message));
        }

        public void error(String message, Throwable throwable) {
            error(throwable);
            logger.error(formatMessage(message), throwable);
        }
    }

    public static LogBuilder forClass(Class<?> clazz) {
        return new LogBuilder(LoggerFactory.getLogger(clazz));
    }

    public static LogBuilder forLogger(Logger logger) {
        return new LogBuilder(logger);
    }

    public static void clearMDC() {
        MDC.clear();
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // Common logging patterns
    public static class Patterns {

        public static void logApiRequest(Logger logger, String operation, String transactionId, Object request) {
            StructuredLogger.forLogger(logger)
                .operation(operation)
                .transactionId(transactionId)
                .requestId(generateRequestId())
                .field("requestPayload", request)
                .timestamp()
                .info("API request received");
        }

        public static void logApiResponse(Logger logger, String operation, String transactionId,
                                        Object response, long responseTimeMs, int httpStatus) {
            StructuredLogger.forLogger(logger)
                .operation(operation)
                .transactionId(transactionId)
                .field("responsePayload", response)
                .responseTime(responseTimeMs)
                .httpStatus(httpStatus)
                .timestamp()
                .info("API response sent");
        }

        public static void logPhonepeApiCall(Logger logger, String operation, String endpoint,
                                           Object request, String phonepeTransactionId) {
            StructuredLogger.forLogger(logger)
                .operation(operation)
                .endpoint(endpoint)
                .phonepeTransactionId(phonepeTransactionId)
                .field("phonepeRequest", request)
                .timestamp()
                .info("PhonePe API call initiated");
        }

        public static void logPhonepeApiResponse(Logger logger, String operation, String endpoint,
                                               Object response, long responseTimeMs, int httpStatus) {
            StructuredLogger.forLogger(logger)
                .operation(operation)
                .endpoint(endpoint)
                .field("phonepeResponse", response)
                .responseTime(responseTimeMs)
                .httpStatus(httpStatus)
                .timestamp()
                .info("PhonePe API response received");
        }

        public static void logError(Logger logger, String operation, String transactionId,
                                  String errorMessage, Throwable throwable) {
            StructuredLogger.forLogger(logger)
                .operation(operation)
                .transactionId(transactionId)
                .error(throwable)
                .timestamp()
                .error(errorMessage, throwable);
        }

        public static void logTransactionStatusChange(Logger logger, String transactionId,
                                                    String oldStatus, String newStatus) {
            StructuredLogger.forLogger(logger)
                .transactionId(transactionId)
                .field("oldStatus", oldStatus)
                .field("newStatus", newStatus)
                .operation("STATUS_CHANGE")
                .timestamp()
                .info("Transaction status changed");
        }

        public static void logSecurityEvent(Logger logger, String eventType, String details, String ipAddress, String userAgent) {
            StructuredLogger.forLogger(logger)
                .operation("SECURITY_EVENT")
                .field("eventType", eventType)
                .field("details", details)
                .field("ipAddress", ipAddress)
                .field("userAgent", userAgent)
                .requestId(generateRequestId())
                .timestamp()
                .warn("Security event detected");
        }

        public static void logPerformanceMetric(Logger logger, String operation, String component,
                                              long executionTimeMs, boolean isSlowQuery) {
            LogBuilder builder = StructuredLogger.forLogger(logger)
                .operation("PERFORMANCE_METRIC")
                .field("component", component)
                .field("executionTimeMs", executionTimeMs)
                .field("isSlowQuery", isSlowQuery)
                .timestamp();

            if (isSlowQuery) {
                builder.warn("Slow performance detected for operation: " + operation);
            } else {
                builder.debug("Performance metric recorded for operation: " + operation);
            }
        }

        public static void logDataAccess(Logger logger, String operation, String tableName,
                                       String query, long executionTimeMs) {
            StructuredLogger.forLogger(logger)
                .operation("DATA_ACCESS")
                .field("tableName", tableName)
                .field("queryType", operation)
                .field("query", query)
                .field("executionTimeMs", executionTimeMs)
                .timestamp()
                .debug("Database operation executed");
        }

        public static void logBusinessEvent(Logger logger, String eventType, String transactionId,
                                          Object eventData) {
            StructuredLogger.forLogger(logger)
                .operation("BUSINESS_EVENT")
                .transactionId(transactionId)
                .field("eventType", eventType)
                .field("eventData", eventData)
                .timestamp()
                .info("Business event occurred");
        }

        // Service-specific logging patterns
        public static void logServiceMethodStart(Logger logger, String serviceName, String methodName,
                                               String transactionId, Object requestParams) {
            StructuredLogger.forLogger(logger)
                .operation("SERVICE_METHOD_START")
                .field("serviceName", serviceName)
                .field("methodName", methodName)
                .transactionId(transactionId)
                .field("requestParams", requestParams)
                .timestamp()
                .debug("Service method execution started");
        }

        public static void logServiceMethodEnd(Logger logger, String serviceName, String methodName,
                                             String transactionId, Object result, long executionTimeMs) {
            StructuredLogger.forLogger(logger)
                .operation("SERVICE_METHOD_END")
                .field("serviceName", serviceName)
                .field("methodName", methodName)
                .transactionId(transactionId)
                .field("result", result)
                .responseTime(executionTimeMs)
                .timestamp()
                .debug("Service method execution completed");
        }

        public static void logExternalApiCall(Logger logger, String apiName, String endpoint,
                                            String httpMethod, Object requestData, String correlationId) {
            StructuredLogger.forLogger(logger)
                .operation("EXTERNAL_API_CALL")
                .field("apiName", apiName)
                .endpoint(endpoint)
                .httpMethod(httpMethod)
                .field("requestData", requestData)
                .field("correlationId", correlationId)
                .timestamp()
                .info("External API call initiated");
        }

        public static void logExternalApiResponse(Logger logger, String apiName, String endpoint,
                                                Object responseData, long responseTimeMs, int httpStatus,
                                                String correlationId) {
            StructuredLogger.forLogger(logger)
                .operation("EXTERNAL_API_RESPONSE")
                .field("apiName", apiName)
                .endpoint(endpoint)
                .field("responseData", responseData)
                .responseTime(responseTimeMs)
                .httpStatus(httpStatus)
                .field("correlationId", correlationId)
                .timestamp()
                .info("External API response received");
        }

        public static void logDatabaseOperation(Logger logger, String operation, String tableName,
                                              Object query, long executionTimeMs, int rowsAffected) {
            StructuredLogger.forLogger(logger)
                .operation("DATABASE_OPERATION")
                .field("dbOperation", operation)
                .field("tableName", tableName)
                .field("query", query)
                .responseTime(executionTimeMs)
                .field("rowsAffected", rowsAffected)
                .timestamp()
                .debug("Database operation executed");
        }

        public static void logQrCodeGeneration(Logger logger, String transactionId, String qrString,
                                             int imageSizeBytes, long generationTimeMs) {
            StructuredLogger.forLogger(logger)
                .operation("QR_CODE_GENERATION")
                .transactionId(transactionId)
                .field("qrStringLength", qrString.length())
                .field("imageSizeBytes", imageSizeBytes)
                .responseTime(generationTimeMs)
                .timestamp()
                .info("QR code generated successfully");
        }

        public static void logPaymentStatusChange(Logger logger, String transactionId, String oldStatus,
                                                String newStatus, String paymentMethod, String reason) {
            StructuredLogger.forLogger(logger)
                .operation("PAYMENT_STATUS_CHANGE")
                .transactionId(transactionId)
                .field("oldStatus", oldStatus)
                .field("newStatus", newStatus)
                .field("paymentMethod", paymentMethod)
                .field("reason", reason)
                .timestamp()
                .info("Payment status changed");
        }

        public static void logMerchantActivity(Logger logger, String merchantId, String activity,
                                             Object activityData, String ipAddress) {
            StructuredLogger.forLogger(logger)
                .operation("MERCHANT_ACTIVITY")
                .merchantId(merchantId)
                .field("activity", activity)
                .field("activityData", activityData)
                .field("ipAddress", ipAddress)
                .timestamp()
                .info("Merchant activity logged");
        }

        public static void logConfigurationChange(Logger logger, String configKey, String oldValue,
                                                String newValue, String changedBy) {
            StructuredLogger.forLogger(logger)
                .operation("CONFIGURATION_CHANGE")
                .field("configKey", configKey)
                .field("oldValue", oldValue)
                .field("newValue", newValue)
                .field("changedBy", changedBy)
                .timestamp()
                .warn("Configuration changed");
        }

        public static void logWebhookEvent(Logger logger, String webhookType, String transactionId,
                                         Object payload, String sourceIp, boolean isValidated) {
            StructuredLogger.forLogger(logger)
                .operation("WEBHOOK_EVENT")
                .field("webhookType", webhookType)
                .transactionId(transactionId)
                .field("payload", payload)
                .field("sourceIp", sourceIp)
                .field("isValidated", isValidated)
                .timestamp()
                .info("Webhook event received");
        }
    }
}