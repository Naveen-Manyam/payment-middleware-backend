package com.phonepe.payment.paymentlink.exception;

import com.phonepe.payment.edc.exception.EdcApiException;
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
 * Exception handler specific to Payment Link operations.
 *
 * <p>This controller advice handles exceptions that occur specifically within
 * the Payment Link module. It provides specialized error handling and response
 * formatting for payment link operations including link generation, transaction
 * processing, cancellation, and refund operations.</p>
 *
 * <p>Handled Exception Types:
 * <ul>
 *   <li>PaymentLinkApiException - Business logic errors specific to Payment Link operations</li>
 *   <li>RuntimeException - Runtime errors in Payment Link processing</li>
 *   <li>Generic Exception - Fallback for any unhandled Payment Link exceptions</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkApiException
 * @see TrackExceptionService
 */
@RestControllerAdvice(basePackages = "com.phonepe.payment.paymentlink")
@Slf4j
public class PaymentLinkGlobalExceptionHandler {

    @Autowired
    private TrackExceptionService trackExceptionService;

    @ExceptionHandler(PaymentLinkApiException.class)
    public ResponseEntity<?> handlePhonePeApi(PaymentLinkApiException ex) {
        HttpStatus status;
        switch (ex.getCode()) {
            case BAD_REQUEST -> status = HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> status = HttpStatus.UNAUTHORIZED;
            case TRANSACTION_NOT_FOUND -> status = HttpStatus.NOT_FOUND;
            default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        StructuredLogger.forLogger(log)
            .operation("PAYMENT_LINK_API_EXCEPTION")
            .field("exceptionType", "PaymentLinkApiException")
            .field("errorCode", ex.getCode().name())
            .field("httpStatus", status.value())
            .error(ex)
            .timestamp()
            .error("Payment Link API exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(status)
                .body(new FailureApiResponse<>(false, ex.getCode().name(), ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        StructuredLogger.forLogger(log)
            .operation("PAYMENT_LINK_RUNTIME_EXCEPTION")
            .field("exceptionType", "RuntimeException")
            .field("httpStatus", HttpStatus.BAD_REQUEST.value())
            .error(ex)
            .timestamp()
            .error("Payment Link runtime exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FailureApiResponse<>(false, "BUSINESS_ERROR", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        StructuredLogger.forLogger(log)
            .operation("PAYMENT_LINK_GENERAL_EXCEPTION")
            .field("exceptionType", "GeneralException")
            .field("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(ex)
            .timestamp()
            .error("Payment Link general exception occurred", ex);

        trackExceptionService.logException(ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FailureApiResponse<>(false, "INTERNAL_ERROR", "Something went wrong", null));
    }
}
