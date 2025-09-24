package com.phonepe.payment.dqr.constants;

public enum DqrResponseCode {
    INVALID_TRANSACTION_ID,	//Duplicate TransactionID
    BAD_REQUEST, 	//Invalid request payload
    UNAUTHORIZED,
    AUTHORIZATION_FAILED,	//Incorrect X-VERIFY header
    INTERNAL_SERVER_ERROR,	//Something went wrong
    SUCCESS,	//API successful
    PAYMENT_ALREADY_COMPLETED,
    TRANSACTION_NOT_FOUND,	//Payment not initiated inside PhonePe
    PAYMENT_SUCCESS,	//Payment is successful
    PAYMENT_ERROR,	//Payment failed
    PAYMENT_PENDING,	//Payment is pending. It does not indicate failed payment.
    PAYMENT_CANCELLED,	//Payment cancelled by merchant
    PAYMENT_DECLINED,	//Payment declined by user
    TIMED_OUT,
    DUPLICATE_TXN_REQUEST,
    EXCESS_REFUND_AMOUNT,
    WALLLET_NOT_ACTIVATED;


    public static DqrResponseCode fromString(String code) {
        try {
            return DqrResponseCode.valueOf(code);
        } catch (Exception e) {
            return null;
        }
    }
}
