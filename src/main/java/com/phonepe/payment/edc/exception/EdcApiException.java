package com.phonepe.payment.edc.exception;

import com.phonepe.payment.edc.constants.EdcResponseCode;
import lombok.Getter;

/**
 * Custom exception for Electronic Data Capture (EDC) API-specific errors.
 *
 * <p>This exception is thrown when EDC-related operations encounter
 * business logic errors, validation failures, or API communication
 * issues specific to the Electronic Data Capture payment module.</p>
 *
 * <p>The exception includes a structured error code from {@link EdcResponseCode}
 * that categorizes the type of error and provides context for error handling
 * and user communication.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see EdcResponseCode
 * @see RuntimeException
 */
@Getter
public class EdcApiException extends RuntimeException {
    private final EdcResponseCode code;

    public EdcApiException(EdcResponseCode code, String message) {
        super(message);
        this.code = code;
    }
}
