package com.phonepe.payment.collect.constants;

import lombok.Getter;

/**
 * Enumeration of response codes for PhonePe Collect API operations.
 *
 * <p>This enum defines standard response codes used across collect call
 * transactions, providing consistent error handling and response mapping
 * for different scenarios in collect payment processing.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@Getter
public enum CollectResponseCode {

    SUCCESS("Request completed successfully"),
    BAD_REQUEST("Invalid request parameters"),
    UNAUTHORIZED("Authentication failed"),
    NOT_FOUND("Resource not found"),
    INTERNAL_SERVER_ERROR("Internal server error occurred"),
    PHONEPE_API_ERROR("PhonePe API error"),
    NETWORK_ERROR("Network communication error"),
    INVALID_TRANSACTION("Invalid transaction details"),
    PAYMENT_FAILED("Payment processing failed"),
    EXPIRED_TRANSACTION("Transaction has expired");

    private final String message;

    CollectResponseCode(String message) {
        this.message = message;
    }

    public static CollectResponseCode fromString(String code) {
        try {
            return CollectResponseCode.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }

}