package com.phonepe.payment.collect.exception;

import com.phonepe.payment.collect.constants.CollectResponseCode;
import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.exception.TrackExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for PhonePe Collect API operations.
 *
 * <p>This class provides centralized exception handling for all collect-related
 * endpoints, converting exceptions into appropriate HTTP responses with proper
 * error codes and user-friendly messages.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@ControllerAdvice
@Slf4j
public class CollectGlobalExceptionHandler {

    @Autowired
    private TrackExceptionService trackExceptionService;

    @ExceptionHandler(CollectApiException.class)
    public ResponseEntity<?> handleCollectApiException(CollectApiException ex) {
        trackExceptionService.logException(ex);

        StructuredLogger.forLogger(log)
            .operation("COLLECT_API_EXCEPTION")
            .field("responseCode", ex.getResponseCode().name())
            .field("userMessage", ex.getUserMessage())
            .error(ex)
            .timestamp()
            .error("Collect API exception occurred", ex);

        HttpStatus httpStatus = mapCollectResponseCodeToHttpStatus(ex.getResponseCode());

        FailureApiResponse<Object> response = FailureApiResponse.builder()
            .success(false)
            .code(ex.getResponseCode().name())
            .message(ex.getUserMessage())
            .build();

        return ResponseEntity.status(httpStatus).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FailureApiResponse> handleGeneralException(Exception ex) {
        trackExceptionService.logException(ex);

        StructuredLogger.forLogger(log)
            .operation("COLLECT_GENERAL_EXCEPTION")
            .error(ex)
            .timestamp()
            .error("Unexpected error in collect API", ex);

        FailureApiResponse response = FailureApiResponse.builder()
            .success(false)
            .code(CollectResponseCode.INTERNAL_SERVER_ERROR.name())
            .message("An unexpected error occurred. Please try again later.")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapCollectResponseCodeToHttpStatus(CollectResponseCode responseCode) {
        return switch (responseCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}