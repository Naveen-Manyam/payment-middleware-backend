package com.phonepe.payment.staticqr.constants;

public enum StaticQRResponseCode {
    SUCCESS,             // Request completed successfully
    BAD_REQUEST,         // Request validation failed
    INTERNAL_SERVER_ERROR, // PhonePe server error
    UNAUTHORIZED,        // X-VERIFY incorrect
    TRANSACTION_NOT_FOUND; // Transaction doesn’t exist

    public static StaticQRResponseCode fromString(String code) {
        try {
            return StaticQRResponseCode.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }
}
