package com.phonepe.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.collect.constants.CollectResponseCode;
import com.phonepe.payment.collect.exception.CollectApiException;
import com.phonepe.payment.dqr.constants.DqrResponseCode;
import com.phonepe.payment.dqr.exception.DqrApiException;
import com.phonepe.payment.edc.constants.EdcResponseCode;
import com.phonepe.payment.edc.exception.EdcApiException;
import com.phonepe.payment.paymentlink.constants.PaymentLinkResponseCode;
import com.phonepe.payment.paymentlink.exception.PaymentLinkApiException;
import com.phonepe.payment.staticqr.constants.StaticQRResponseCode;
import com.phonepe.payment.staticqr.exception.StaticQRApiException;
import com.phonepe.payment.exception.TrackExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Common utility functions for service operations across the application.
 *
 * <p>This utility class provides commonly used helper functions for JSON processing,
 * transaction ID generation, timestamp management, and result wrapping. It serves
 * as a centralized location for shared functionality across all service layers.</p>
 *
 * <p>Provided Utilities:
 * <ul>
 *   <li>JSON serialization and deserialization</li>
 *   <li>Transaction ID generation</li>
 *   <li>Current timestamp retrieval</li>
 *   <li>Service operation result wrapping</li>
 *   <li>Response parsing and error handling</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see ObjectMapper
 * @see GenerateTransactionId
 */
@Component
@Slf4j
public class CommonServiceUtils {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GenerateTransactionId generateTransactionId;

    @Autowired
    private TrackExceptionService trackExceptionService;

    @Autowired
    private RequestValidationService requestValidationService;

    public String convertToJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public String generateTransactionId(String numeric) {
        return GenerateTransactionId.generateTransactionId(numeric);
    }

    public LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    public <T> T parseResponse(String responseJson, Class<T> responseClass) throws JsonProcessingException {
        return objectMapper.readValue(responseJson, responseClass);
    }

    /**
     * Validates required fields in a request object.
     *
     * @param request The request object to validate
     * @param requiredFields Array of field names that must be present and non-null
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateRequiredFields(Object request, String... requiredFields) {
        return requestValidationService.validateRequiredFields(request, requiredFields);
    }

    /**
     * Validates business rules for common field types.
     *
     * @param request The request object to validate
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateBusinessRules(Object request) {
        return requestValidationService.validateBusinessRules(request);
    }

    /**
     * Comprehensive validation method that checks both required fields and business rules.
     *
     * @param request The request object to validate
     * @param requiredFields Array of field names that must be present and non-null
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateRequest(Object request, String... requiredFields) {
        return requestValidationService.validateRequest(request, requiredFields);
    }

    /**
     * Global PhonePe API error handler for DQR service operations.
     *
     * @param e The exception thrown
     * @param transactionId The transaction ID for logging context
     * @param operation The operation being performed (e.g., "INIT", "CANCEL", "REFUND", "STATUS_CHECK")
     * @param logger The logger to use for error logging
     * @throws DqrApiException Mapped exception with appropriate response code
     */
    public void handleDqrApiError(Exception e, String transactionId, String operation, Logger logger) {
        trackExceptionService.logException(e);

        if (e.getMessage() != null && e.getMessage().contains("PhonePe API Error: HTTP")) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("400") && errorMessage.contains("DUPLICATE_TXN_REQUEST")) {
                StructuredLogger.Patterns.logError(logger, "DQR_SERVICE_" + operation + "_DUPLICATE_ERROR",
                        transactionId, "Duplicate transaction request for DQR " + operation, e);
                throw new DqrApiException(DqrResponseCode.DUPLICATE_TXN_REQUEST, "Duplicate transaction request");
            } else if (errorMessage.contains("401") && errorMessage.contains("UNAUTHORIZED")) {
                StructuredLogger.Patterns.logError(logger, "DQR_SERVICE_" + operation + "_AUTH_ERROR",
                        transactionId, "Authentication failed for DQR " + operation, e);
                throw new DqrApiException(DqrResponseCode.UNAUTHORIZED, "Authentication failed");
            } else if (errorMessage.contains("404")) {
                StructuredLogger.Patterns.logError(logger, "DQR_SERVICE_" + operation + "_NOT_FOUND_ERROR",
                        transactionId, "Transaction not found for DQR " + operation, e);
                throw new DqrApiException(DqrResponseCode.TRANSACTION_NOT_FOUND, "Transaction not found");
            } else if (errorMessage.contains("400")) {
                StructuredLogger.Patterns.logError(logger, "DQR_SERVICE_" + operation + "_BAD_REQUEST_ERROR",
                        transactionId, "Bad request for DQR " + operation, e);
                throw new DqrApiException(DqrResponseCode.BAD_REQUEST, "Invalid request parameters");
            }
        }

        StructuredLogger.Patterns.logError(logger, "DQR_SERVICE_" + operation + "_UNEXPECTED_ERROR",
                transactionId, "Unexpected error during DQR " + operation, e);
        throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
    }

    /**
     * Global PhonePe API error handler for EDC service operations.
     */
    public void handleEdcApiError(Exception e, String transactionId, String operation, Logger logger) {
        trackExceptionService.logException(e);

        if (e.getMessage() != null && e.getMessage().contains("PhonePe API Error: HTTP")) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("400") && errorMessage.contains("DUPLICATE_TXN_REQUEST")) {
                StructuredLogger.Patterns.logError(logger, "EDC_SERVICE_" + operation + "_DUPLICATE_ERROR",
                        transactionId, "Duplicate transaction request for EDC " + operation, e);
                throw new EdcApiException(EdcResponseCode.BAD_REQUEST, "Duplicate transaction request");
            } else if (errorMessage.contains("401") && errorMessage.contains("UNAUTHORIZED")) {
                StructuredLogger.Patterns.logError(logger, "EDC_SERVICE_" + operation + "_AUTH_ERROR",
                        transactionId, "Authentication failed for EDC " + operation, e);
                throw new EdcApiException(EdcResponseCode.UNAUTHORIZED, "Authentication failed");
            } else if (errorMessage.contains("404")) {
                StructuredLogger.Patterns.logError(logger, "EDC_SERVICE_" + operation + "_NOT_FOUND_ERROR",
                        transactionId, "Transaction not found for EDC " + operation, e);
                throw new EdcApiException(EdcResponseCode.TRANSACTION_NOT_FOUND, "Transaction not found");
            } else if (errorMessage.contains("400")) {
                StructuredLogger.Patterns.logError(logger, "EDC_SERVICE_" + operation + "_BAD_REQUEST_ERROR",
                        transactionId, "Bad request for EDC " + operation, e);
                throw new EdcApiException(EdcResponseCode.BAD_REQUEST, "Invalid request parameters");
            }
        }

        StructuredLogger.Patterns.logError(logger, "EDC_SERVICE_" + operation + "_UNEXPECTED_ERROR",
                transactionId, "Unexpected error during EDC " + operation, e);
        throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
    }

    /**
     * Global PhonePe API error handler for PaymentLink service operations.
     */
    public void handlePaymentLinkApiError(Exception e, String transactionId, String operation, Logger logger) {
        trackExceptionService.logException(e);

        if (e.getMessage() != null && e.getMessage().contains("PhonePe API Error: HTTP")) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("400") && errorMessage.contains("DUPLICATE_TXN_REQUEST")) {
                StructuredLogger.Patterns.logError(logger, "PAYMENT_LINK_SERVICE_" + operation + "_DUPLICATE_ERROR",
                        transactionId, "Duplicate transaction request for PaymentLink " + operation, e);
                throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Duplicate transaction request");
            } else if (errorMessage.contains("401") && errorMessage.contains("UNAUTHORIZED")) {
                StructuredLogger.Patterns.logError(logger, "PAYMENT_LINK_SERVICE_" + operation + "_AUTH_ERROR",
                        transactionId, "Authentication failed for PaymentLink " + operation, e);
                throw new PaymentLinkApiException(PaymentLinkResponseCode.UNAUTHORIZED, "Authentication failed");
            } else if (errorMessage.contains("404")) {
                StructuredLogger.Patterns.logError(logger, "PAYMENT_LINK_SERVICE_" + operation + "_NOT_FOUND_ERROR",
                        transactionId, "Transaction not found for PaymentLink " + operation, e);
                throw new PaymentLinkApiException(PaymentLinkResponseCode.TRANSACTION_NOT_FOUND, "Transaction not found");
            } else if (errorMessage.contains("400")) {
                StructuredLogger.Patterns.logError(logger, "PAYMENT_LINK_SERVICE_" + operation + "_BAD_REQUEST_ERROR",
                        transactionId, "Bad request for PaymentLink " + operation, e);
                throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Invalid request parameters");
            }
        }

        StructuredLogger.Patterns.logError(logger, "PAYMENT_LINK_SERVICE_" + operation + "_UNEXPECTED_ERROR",
                transactionId, "Unexpected error during PaymentLink " + operation, e);
        throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
    }

    /**
     * Global PhonePe API error handler for StaticQR service operations.
     */
    public void handleStaticQRApiError(Exception e, String transactionId, String operation, Logger logger) {
        trackExceptionService.logException(e);

        if (e.getMessage() != null && e.getMessage().contains("PhonePe API Error: HTTP")) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("400") && errorMessage.contains("DUPLICATE_TXN_REQUEST")) {
                StructuredLogger.Patterns.logError(logger, "STATIC_QR_SERVICE_" + operation + "_DUPLICATE_ERROR",
                        transactionId, "Duplicate transaction request for StaticQR " + operation, e);
                throw new StaticQRApiException(StaticQRResponseCode.BAD_REQUEST, "Duplicate transaction request");
            } else if (errorMessage.contains("401") && errorMessage.contains("UNAUTHORIZED")) {
                StructuredLogger.Patterns.logError(logger, "STATIC_QR_SERVICE_" + operation + "_AUTH_ERROR",
                        transactionId, "Authentication failed for StaticQR " + operation, e);
                throw new StaticQRApiException(StaticQRResponseCode.UNAUTHORIZED, "Authentication failed");
            } else if (errorMessage.contains("404")) {
                StructuredLogger.Patterns.logError(logger, "STATIC_QR_SERVICE_" + operation + "_NOT_FOUND_ERROR",
                        transactionId, "Transaction not found for StaticQR " + operation, e);
                throw new StaticQRApiException(StaticQRResponseCode.TRANSACTION_NOT_FOUND, "Transaction not found");
            } else if (errorMessage.contains("400")) {
                StructuredLogger.Patterns.logError(logger, "STATIC_QR_SERVICE_" + operation + "_BAD_REQUEST_ERROR",
                        transactionId, "Bad request for StaticQR " + operation, e);
                throw new StaticQRApiException(StaticQRResponseCode.BAD_REQUEST, "Invalid request parameters");
            }
        }

        StructuredLogger.Patterns.logError(logger, "STATIC_QR_SERVICE_" + operation + "_UNEXPECTED_ERROR",
                transactionId, "Unexpected error during StaticQR " + operation, e);
        throw new StaticQRApiException(StaticQRResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
    }

    /**
     * Global PhonePe API error handler for Collect service operations.
     */
    public void handleCollectApiError(Exception e, String transactionId, String operation, Logger logger) {
        trackExceptionService.logException(e);

        if (e.getMessage() != null && e.getMessage().contains("PhonePe API Error: HTTP")) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("400") && errorMessage.contains("DUPLICATE_TXN_REQUEST")) {
                StructuredLogger.Patterns.logError(logger, "COLLECT_CALL_SERVICE_" + operation + "_DUPLICATE_ERROR",
                        transactionId, "Duplicate transaction request for COLLECT_CALL " + operation, e);
                throw new CollectApiException(CollectResponseCode.BAD_REQUEST, "Duplicate transaction request");
            } else if (errorMessage.contains("401") && errorMessage.contains("UNAUTHORIZED")) {
                StructuredLogger.Patterns.logError(logger, "COLLECT_CALL_SERVICE_" + operation + "_AUTH_ERROR",
                        transactionId, "Authentication failed for COLLECT_CALL " + operation, e);
                throw new CollectApiException(CollectResponseCode.UNAUTHORIZED, "Authentication failed");
            } else if (errorMessage.contains("404")) {
                StructuredLogger.Patterns.logError(logger, "COLLECT_CALL_SERVICE_" + operation + "_NOT_FOUND_ERROR",
                        transactionId, "Transaction not found for COLLECT_CALL " + operation, e);
                throw new CollectApiException(CollectResponseCode.NOT_FOUND, "Transaction not found");
            } else if (errorMessage.contains("400")) {
                StructuredLogger.Patterns.logError(logger, "COLLECT_CALL_SERVICE_" + operation + "_BAD_REQUEST_ERROR",
                        transactionId, "Bad request for COLLECT_CALL " + operation, e);
                throw new CollectApiException(CollectResponseCode.BAD_REQUEST, "Invalid request parameters");
            }
        }

        StructuredLogger.Patterns.logError(logger, "COLLECT_CALL_SERVICE_" + operation + "_UNEXPECTED_ERROR",
                transactionId, "Unexpected error during COLLECT_CALL " + operation, e);
        throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
    }

    public static class ServiceOperationResult<T> {
        private final boolean success;
        private final T data;
        private final String errorCode;
        private final String errorMessage;
        private final Exception exception;

        private ServiceOperationResult(boolean success, T data, String errorCode, String errorMessage, Exception exception) {
            this.success = success;
            this.data = data;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }

        public static <T> ServiceOperationResult<T> success(T data) {
            return new ServiceOperationResult<>(true, data, null, null, null);
        }

        public static <T> ServiceOperationResult<T> failure(String errorCode, String errorMessage, Exception exception) {
            return new ServiceOperationResult<>(false, null, errorCode, errorMessage, exception);
        }

        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public Exception getException() { return exception; }
    }
}