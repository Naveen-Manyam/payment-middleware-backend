package com.phonepe.payment.staticqr.exception;

import com.phonepe.payment.staticqr.constants.StaticQRResponseCode;
import lombok.Getter;

/**
 * Custom exception for Static QR API-specific errors.
 *
 * <p>This exception is thrown when Static QR-related operations encounter
 * business logic errors, validation failures, or API communication
 * issues specific to the Static QR payment module.</p>
 *
 * <p>The exception includes a structured error code from {@link StaticQRResponseCode}
 * that categorizes the type of error and provides context for error handling
 * and user communication.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see StaticQRResponseCode
 * @see RuntimeException
 */
@Getter
public class StaticQRApiException extends RuntimeException {
    private final StaticQRResponseCode code;

    public StaticQRApiException(StaticQRResponseCode code, String message) {
        super(message);
        this.code = code;
    }
}
