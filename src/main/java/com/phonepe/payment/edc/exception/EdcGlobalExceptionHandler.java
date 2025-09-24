package com.phonepe.payment.edc.exception;

import com.phonepe.payment.exception.TrackExceptionService;
import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.util.StructuredLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler specific to Electronic Data Capture (EDC) payment operations.
 *
 * <p>This controller advice handles exceptions that occur specifically within
 * the EDC payment module. It provides specialized error handling and response
 * formatting for EDC-related operations including card payments, transaction
 * processing, and settlement operations.</p>
 *
 * <p>Handled Exception Types:
 * <ul>
 *   <li>EdcApiException - Business logic errors specific to EDC operations</li>
 *   <li>RuntimeException - Runtime errors in EDC processing</li>
 *   <li>Generic Exception - Fallback for any unhandled EDC exceptions</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see EdcApiException
 * @see TrackExceptionService
 */
@RestControllerAdvice(basePackages = "com.phonepe.payment.edc")
@Slf4j
public class EdcGlobalExceptionHandler {

    @Autowired
    private TrackExceptionService trackExceptionService;


    @ExceptionHandler(EdcApiException.class)
    public ResponseEntity<?> handlePhonePeApi(EdcApiException ex) {
        HttpStatus status;
        switch (ex.getCode()) {
            case BAD_REQUEST -> status = HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> status = HttpStatus.UNAUTHORIZED;
            case TRANSACTION_NOT_FOUND -> status = HttpStatus.NOT_FOUND;
            default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        StructuredLogger.forLogger(log)
            .operation("EDC_API_EXCEPTION")
            .field("exceptionType", "EdcApiException")
            .field("errorCode", ex.getCode().name())
            .field("httpStatus", status.value())
            .error(ex)
            .timestamp()
            .error("EDC API exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(status)
                .body(new FailureApiResponse<>(false, ex.getCode().name(), ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        StructuredLogger.forLogger(log)
            .operation("EDC_RUNTIME_EXCEPTION")
            .field("exceptionType", "RuntimeException")
            .field("httpStatus", HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("EDC runtime exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "BUSINESS_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        StructuredLogger.forLogger(log)
            .operation("EDC_GENERAL_EXCEPTION")
            .field("exceptionType", "GeneralException")
            .field("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(ex)
            .timestamp()
            .error("EDC general exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FailureApiResponse<>(false, "INTERNAL_ERROR", "Something went wrong", null));
    }
}
