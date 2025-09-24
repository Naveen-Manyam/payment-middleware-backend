package com.phonepe.payment.collect.exception;

import com.phonepe.payment.collect.constants.CollectResponseCode;
import lombok.Getter;

/**
 * Custom exception class for PhonePe Collect API specific errors.
 *
 * <p>This exception is thrown when collect call API operations encounter
 * business logic errors, validation failures, or external service failures
 * specific to PhonePe collect transactions.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectResponseCode
 */
@Getter
public class CollectApiException extends RuntimeException {

    private final CollectResponseCode responseCode;
    private final String userMessage;

    public CollectApiException(CollectResponseCode responseCode, String userMessage) {
        super(userMessage);
        this.responseCode = responseCode;
        this.userMessage = userMessage;
    }

    public CollectApiException(CollectResponseCode responseCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.responseCode = responseCode;
        this.userMessage = userMessage;
    }

}