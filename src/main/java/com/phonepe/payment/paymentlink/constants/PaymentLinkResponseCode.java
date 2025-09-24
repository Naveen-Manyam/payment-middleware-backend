package com.phonepe.payment.paymentlink.constants;

public enum PaymentLinkResponseCode {
    SUCCESS,             // Request completed successfully
    BAD_REQUEST,         // Request validation failed
    INTERNAL_SERVER_ERROR, // PhonePe server error
    UNAUTHORIZED,        // X-VERIFY incorrect
    TRANSACTION_NOT_FOUND; // Transaction doesn’t exist

    public static PaymentLinkResponseCode fromString(String code) {
        try {
            return PaymentLinkResponseCode.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }
}
