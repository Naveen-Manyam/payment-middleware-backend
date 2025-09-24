package com.phonepe.payment.dqr.exception;


import com.phonepe.payment.dqr.constants.DqrResponseCode;
import lombok.Getter;

/**
 * Custom exception for Dynamic QR (DQR) API-specific errors.
 *
 * <p>This exception is thrown when DQR-related operations encounter
 * business logic errors, validation failures, or API communication
 * issues specific to the Dynamic QR payment module.</p>
 *
 * <p>The exception includes a structured error code from {@link DqrResponseCode}
 * that categorizes the type of error and provides context for error handling
 * and user communication.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see DqrResponseCode
 * @see RuntimeException
 */
@Getter
public class DqrApiException extends RuntimeException {
    private final DqrResponseCode code;

    public DqrApiException(DqrResponseCode code, String message) {
        super(message);
        this.code = code;
    }
}
