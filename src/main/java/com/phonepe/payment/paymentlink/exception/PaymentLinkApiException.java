package com.phonepe.payment.paymentlink.exception;

import com.phonepe.payment.edc.constants.EdcResponseCode;
import com.phonepe.payment.paymentlink.constants.PaymentLinkResponseCode;
import lombok.Getter;

/**
 * Custom exception for Payment Link API-specific errors.
 *
 * <p>This exception is thrown when Payment Link-related operations encounter
 * business logic errors, validation failures, or API communication
 * issues specific to the Payment Link module.</p>
 *
 * <p>The exception includes a structured error code from {@link PaymentLinkResponseCode}
 * that categorizes the type of error and provides context for error handling
 * and user communication.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkResponseCode
 * @see RuntimeException
 */
@Getter
public class PaymentLinkApiException extends RuntimeException {
    private final PaymentLinkResponseCode code;

    public PaymentLinkApiException(PaymentLinkResponseCode code, String message) {
        super(message);
        this.code = code;
    }
}
