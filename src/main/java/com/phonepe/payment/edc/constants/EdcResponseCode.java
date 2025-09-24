package com.phonepe.payment.edc.constants;

public enum EdcResponseCode {
    SUCCESS,             // Request completed successfully
    BAD_REQUEST,         // Request validation failed
    INTERNAL_SERVER_ERROR, // PhonePe server error
    UNAUTHORIZED,        // X-VERIFY incorrect
    TRANSACTION_NOT_FOUND; // Transaction doesn’t exist

    public static EdcResponseCode fromString(String code) {
        try {
            return EdcResponseCode.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }
}
