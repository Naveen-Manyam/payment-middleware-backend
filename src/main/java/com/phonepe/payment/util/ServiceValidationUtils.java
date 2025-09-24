package com.phonepe.payment.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utility for service layer operations.
 *
 * <p>This utility class provides a centralized validation framework for all
 * service layer operations in the PhonePe middleware application. It includes
 * validation for common fields like transaction IDs, merchant IDs, amounts,
 * contact information, and business-specific rules.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Field-level validation with structured error reporting</li>
 *   <li>Business rule validation (refunds, transaction age, etc.)</li>
 *   <li>Pattern-based validation for IDs and contact information</li>
 *   <li>Amount validation with configurable limits</li>
 *   <li>Structured logging for validation failures</li>
 *   <li>Thread-safe validation operations</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see ValidationResult
 * @see StructuredLogger
 */
@Slf4j
public class ServiceValidationUtils {

    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final Pattern MERCHANT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");
    private static final Pattern MOBILE_NUMBER_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String fieldName;

        public ValidationResult(boolean valid, String fieldName, String errorMessage) {
            this.valid = valid;
            this.fieldName = fieldName;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getFieldName() { return fieldName; }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(String fieldName, String errorMessage) {
            return new ValidationResult(false, fieldName, errorMessage);
        }
    }

    public static ValidationResult validateTransactionId(String transactionId, Logger logger) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            logValidationFailure(logger, "transactionId", "null or empty", transactionId);
            return ValidationResult.failure("transactionId", "Transaction ID cannot be null or empty");
        }

        if (!TRANSACTION_ID_PATTERN.matcher(transactionId).matches()) {
            logValidationFailure(logger, "transactionId", "invalid format", transactionId);
            return ValidationResult.failure("transactionId", "Transaction ID must be 8-64 characters, alphanumeric with _ and -");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateMerchantId(String merchantId, Logger logger) {
        if (merchantId == null || merchantId.trim().isEmpty()) {
            logValidationFailure(logger, "merchantId", "null or empty", merchantId);
            return ValidationResult.failure("merchantId", "Merchant ID cannot be null or empty");
        }

        if (!MERCHANT_ID_PATTERN.matcher(merchantId).matches()) {
            logValidationFailure(logger, "merchantId", "invalid format", merchantId);
            return ValidationResult.failure("merchantId", "Merchant ID must be 1-32 characters, alphanumeric with _ and -");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateAmount(Object amountObj, Logger logger) {
        if (amountObj == null) {
            logValidationFailure(logger, "amount", "null", null);
            return ValidationResult.failure("amount", "Amount cannot be null");
        }

        BigDecimal amount;
        try {
            if (amountObj instanceof Number) {
                amount = new BigDecimal(amountObj.toString());
            } else {
                amount = new BigDecimal(amountObj.toString());
            }
        } catch (NumberFormatException e) {
            logValidationFailure(logger, "amount", "invalid number format", amountObj);
            return ValidationResult.failure("amount", "Amount must be a valid number");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logValidationFailure(logger, "amount", "non-positive", amount);
            return ValidationResult.failure("amount", "Amount must be greater than zero");
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            logValidationFailure(logger, "amount", "exceeds maximum", amount);
            return ValidationResult.failure("amount", "Amount cannot exceed ₹10,00,000");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateMobileNumber(String mobileNumber, Logger logger) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            logValidationFailure(logger, "mobileNumber", "null or empty", mobileNumber);
            return ValidationResult.failure("mobileNumber", "Mobile number cannot be null or empty");
        }

        String cleanNumber = mobileNumber.replaceAll("[^0-9]", "");
        if (cleanNumber.length() == 10 && MOBILE_NUMBER_PATTERN.matcher(cleanNumber).matches()) {
            return ValidationResult.success();
        }

        logValidationFailure(logger, "mobileNumber", "invalid format", mobileNumber);
        return ValidationResult.failure("mobileNumber", "Mobile number must be a valid 10-digit Indian number starting with 6-9");
    }

    public static ValidationResult validateEmail(String email, Logger logger) {
        if (email == null || email.trim().isEmpty()) {
            logValidationFailure(logger, "email", "null or empty", email);
            return ValidationResult.failure("email", "Email cannot be null or empty");
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            logValidationFailure(logger, "email", "invalid format", email);
            return ValidationResult.failure("email", "Email must be in valid format");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateStringLength(String value, String fieldName, int minLength, int maxLength, Logger logger) {
        if (value == null) {
            logValidationFailure(logger, fieldName, "null", null);
            return ValidationResult.failure(fieldName, fieldName + " cannot be null");
        }

        if (value.length() < minLength || value.length() > maxLength) {
            logValidationFailure(logger, fieldName, "invalid length", value.length());
            return ValidationResult.failure(fieldName, fieldName + " must be between " + minLength + " and " + maxLength + " characters");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateNotNull(Object value, String fieldName, Logger logger) {
        if (value == null) {
            logValidationFailure(logger, fieldName, "null", null);
            return ValidationResult.failure(fieldName, fieldName + " cannot be null");
        }

        if (value instanceof String && ((String) value).trim().isEmpty()) {
            logValidationFailure(logger, fieldName, "empty string", value);
            return ValidationResult.failure(fieldName, fieldName + " cannot be empty");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateExpiresIn(Integer expiresIn, Logger logger) {
        if (expiresIn == null) {
            logValidationFailure(logger, "expiresIn", "null", null);
            return ValidationResult.failure("expiresIn", "ExpiresIn cannot be null");
        }

        if (expiresIn < 60 || expiresIn > 86400) { // 1 minute to 24 hours
            logValidationFailure(logger, "expiresIn", "out of range", expiresIn);
            return ValidationResult.failure("expiresIn", "ExpiresIn must be between 60 and 86400 seconds");
        }

        return ValidationResult.success();
    }

    public static ValidationResult validateProvider(String provider, Logger logger) {
        if (provider == null || provider.trim().isEmpty()) {
            logValidationFailure(logger, "provider", "null or empty", provider);
            return ValidationResult.failure("provider", "Provider cannot be null or empty");
        }

        // Add specific provider validation logic if needed
        return ValidationResult.success();
    }

    private static void logValidationFailure(Logger logger, String fieldName, String reason, Object value) {
        StructuredLogger.forLogger(logger)
            .operation("VALIDATION_FAILURE")
            .field("fieldName", fieldName)
            .field("failureReason", reason)
            .field("providedValue", value)
            .timestamp()
            .warn("Validation failed for field: " + fieldName);
    }

    public static void logValidationSuccess(Logger logger, String operation, Object validatedObject) {
        StructuredLogger.forLogger(logger)
            .operation("VALIDATION_SUCCESS")
            .field("validationOperation", operation)
            .field("validatedObject", validatedObject.getClass().getSimpleName())
            .timestamp()
            .debug("Validation successful for operation: " + operation);
    }

    public static class BusinessRuleValidator {

        public static ValidationResult validateRefundAmount(BigDecimal refundAmount, BigDecimal originalAmount, Logger logger) {
            if (refundAmount.compareTo(originalAmount) > 0) {
                logValidationFailure(logger, "refundAmount", "exceeds original amount", refundAmount);
                return ValidationResult.failure("refundAmount", "Refund amount cannot exceed original transaction amount");
            }
            return ValidationResult.success();
        }

        public static ValidationResult validateTransactionAge(java.time.LocalDateTime transactionDate, int maxAgeInDays, Logger logger) {
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(maxAgeInDays);
            if (transactionDate.isBefore(cutoff)) {
                logValidationFailure(logger, "transactionDate", "too old for operation", transactionDate);
                return ValidationResult.failure("transactionDate", "Transaction is too old for this operation");
            }
            return ValidationResult.success();
        }

        public static ValidationResult validateMerchantStatus(String merchantStatus, Logger logger) {
            if (!"ACTIVE".equalsIgnoreCase(merchantStatus)) {
                logValidationFailure(logger, "merchantStatus", "inactive merchant", merchantStatus);
                return ValidationResult.failure("merchantStatus", "Merchant is not active");
            }
            return ValidationResult.success();
        }
    }
}