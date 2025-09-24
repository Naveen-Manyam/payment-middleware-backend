package com.phonepe.payment.dqr.controller;

import com.phonepe.payment.dqr.entity.*;
import com.phonepe.payment.dqr.exception.DqrApiException;
import com.phonepe.payment.dqr.service.DqrService;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.util.CommonServiceUtils;
import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.exception.TrackExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see DqrService
 * @see DqrInitializeTransactionRequest
 * @see DqrInitializeTransactionResponse
 * @see StructuredLogger
 */
@RestController
@RequestMapping("/api/phonepe/dqr")
@Slf4j
public class DqrController {

    private final DqrService dqrService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;

    @Autowired
    public DqrController(DqrService dqrService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.dqrService = dqrService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    /**
     * Initializes a Dynamic QR (DQR) transaction.
     * @throws DqrApiException when PhonePe API returns business logic errors
     * @throws IllegalArgumentException when request validation fails
     * @see DqrInitializeTransactionRequest
     * @see DqrService#initTransaction(DqrInitializeTransactionRequest)
     */
    @PostMapping(value = "/init", produces = {MediaType.IMAGE_PNG_VALUE})
    @Operation(summary = "Initialize Payment QR", description = "Generates a dynamic QR for UPI payment using PhonePe by passing an amount.")
    @ApiResponse(responseCode = "200", description = "QR generated successfully", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> initTransaction(@RequestBody DqrInitializeTransactionRequest dqrInitializeTransactionRequest) {
        long startTime = System.currentTimeMillis();
        String requestId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "DQR_INIT",
                    dqrInitializeTransactionRequest.getMerchantId(), dqrInitializeTransactionRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                dqrInitializeTransactionRequest,
                "merchantId", "amount", "storeId", "terminalId","provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = dqrService.initTransaction(dqrInitializeTransactionRequest);

            StructuredLogger.Patterns.logApiResponse(log, "DQR_INIT",
                dqrInitializeTransactionRequest.getTransactionId(), "QR_GENERATED",
                System.currentTimeMillis() - startTime, response.getStatusCode().value());
            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "DQR_INIT",
                dqrInitializeTransactionRequest.getTransactionId(), "Error initializing DQR transaction", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel Payment", description = "Cancels an ongoing transaction using the transaction ID.")
    @ApiResponse(responseCode = "200", description = "Transaction cancelled successfully", content = @Content(schema = @Schema(implementation = DqrCancelTransactionResponse.class)))
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> cancelPayment(@RequestBody DqrCancelTransactionRequest dqrCancelTransactionRequest) {
        long startTime = System.currentTimeMillis();

        try {

            StructuredLogger.Patterns.logApiRequest(log, "DQR_CANCEL",
                    dqrCancelTransactionRequest.getTransactionId(), dqrCancelTransactionRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                dqrCancelTransactionRequest,
                "merchantId", "transactionId","reason","provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = dqrService.cancelTransaction(dqrCancelTransactionRequest);
            StructuredLogger.Patterns.logApiResponse(log, "DQR_CANCEL",
                dqrCancelTransactionRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());
            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "DQR_CANCEL",
                dqrCancelTransactionRequest.getTransactionId(), "Error cancelling DQR transaction", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund Payment", description = "Initiates a refund for a previously completed transaction.")
    @ApiResponse(responseCode = "200", description = "Refund processed successfully", content = @Content(schema = @Schema(implementation = DqrRefundTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid refund request")
    @ApiResponse(responseCode = "404", description = "Original transaction not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> refund(@RequestBody DqrRefundTransactionRequest dqrRefundTransactionRequest) {
        long startTime = System.currentTimeMillis();
        try {
            StructuredLogger.Patterns.logApiRequest(log, "DQR_REFUND",
                    dqrRefundTransactionRequest.getOriginalTransactionId(), dqrRefundTransactionRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                dqrRefundTransactionRequest,
                "merchantId", "originalTransactionId", "amount", "merchantOrderId", "message", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = dqrService.refund(dqrRefundTransactionRequest);
            StructuredLogger.Patterns.logApiResponse(log, "DQR_REFUND",
                dqrRefundTransactionRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "DQR_REFUND",
                dqrRefundTransactionRequest.getTransactionId(), "Error processing DQR refund", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/status")
    @Operation(summary = "Check Payment Status", description = "Fetches the current status of a transaction by transaction ID.")
    @ApiResponse(responseCode = "200", description = "Transaction status retrieved successfully", content = @Content(schema = @Schema(implementation = DqrCheckTransactionStatusResponse.class)))
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentStatus(@RequestBody DqrCheckTransactionStatusRequest dqrCheckTransactionStatusRequest) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "DQR_STATUS_CHECK",
                    dqrCheckTransactionStatusRequest.getTransactionId(), dqrCheckTransactionStatusRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                dqrCheckTransactionStatusRequest,
                "merchantId", "transactionId","provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = dqrService.checkPaymentStatus(dqrCheckTransactionStatusRequest);
            StructuredLogger.Patterns.logApiResponse(log, "DQR_STATUS_CHECK",
                dqrCheckTransactionStatusRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());
            return response;
        } catch (Exception e) {
            StructuredLogger.Patterns.logError(log, "DQR_STATUS_CHECK",
                dqrCheckTransactionStatusRequest.getTransactionId(), "Error checking DQR payment status", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }
}
