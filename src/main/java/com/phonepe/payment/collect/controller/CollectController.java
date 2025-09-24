package com.phonepe.payment.collect.controller;

import com.phonepe.payment.collect.entity.*;
import com.phonepe.payment.collect.service.CollectService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for PhonePe Collect Call payment operations.
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectService
 */
@RestController
@RequestMapping("/api/phonepe/collect")
@Slf4j
public class CollectController {

    private final CollectService collectService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;

    @Autowired
    public CollectController(CollectService collectService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.collectService = collectService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    @PostMapping("/collect-call")
    @Operation(summary = "Initiate Collect Call", description = "Initiates a collect call transaction to customer mobile number.")
    @ApiResponse(responseCode = "200", description = "Collect call initiated successfully", content = @Content(schema = @Schema(implementation = CollectCallResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> initiateCollectCall(@RequestBody CollectCallRequest collectCallRequest) {
        long startTime = System.currentTimeMillis();
        String requestId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "COLLECT_CALL_INIT",
                    collectCallRequest.getMerchantId(), collectCallRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                collectCallRequest,
                "merchantId", "amount", "instrumentType", "instrumentReference", "provider", "expiresIn", "storeId"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = collectService.initiateCollectCall(collectCallRequest);

            StructuredLogger.forLogger(log)
                .operation("COLLECT_CALL_RESPONSE")
                .requestId(requestId)
                .field("merchantId", collectCallRequest.getMerchantId())
                .field("transactionId", collectCallRequest.getTransactionId())
                .responseTime(System.currentTimeMillis() - startTime)
                .httpStatus(response.getStatusCode().value())
                .timestamp()
                .info("Collect call response sent");

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("COLLECT_CALL_ERROR")
                .requestId(requestId)
                .field("merchantId", collectCallRequest.getMerchantId())
                .field("transactionId", collectCallRequest.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Error processing collect call request", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel Collect Call Transaction", description = "Cancels an active Collect Call transaction using PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Transaction cancelled successfully", content = @Content(schema = @Schema(implementation = CollectCallCancelTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> cancelTransaction(@RequestBody CollectCallCancelTransactionRequest cancelRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "COLLECT_CALL_CANCEL",
                    cancelRequest.getTransactionId(), cancelRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                cancelRequest,
                "merchantId", "transactionId", "provider", "reason"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = collectService.cancelTransaction(cancelRequest);

            StructuredLogger.Patterns.logApiResponse(log, "COLLECT_CALL_CANCEL",
                    cancelRequest.getTransactionId(), response.getBody(),
                    System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "COLLECT_CALL_CANCEL",
                    cancelRequest.getTransactionId(), "Error cancelling Collect Call transaction", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/status")
    @Operation(summary = "Check Collect Call Transaction Status", description = "Retrieves the current status of a Collect Call transaction from PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully", content = @Content(schema = @Schema(implementation = CollectCallCheckTransactionStatusResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> checkPaymentStatus(@RequestBody CollectCallCheckTransactionStatusRequest statusRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "COLLECT_CALL_STATUS_CHECK",
                    statusRequest.getTransactionId(), statusRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                statusRequest,
                "merchantId", "transactionId", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = collectService.checkPaymentStatus(statusRequest);

            StructuredLogger.Patterns.logApiResponse(log, "COLLECT_CALL_STATUS_CHECK",
                    statusRequest.getTransactionId(), response.getBody(),
                    System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "COLLECT_CALL_STATUS_CHECK",
                    statusRequest.getTransactionId(), "Error checking Collect Call transaction status", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund Collect Call Transaction", description = "Processes a refund for a completed Collect Call transaction using PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Refund processed successfully", content = @Content(schema = @Schema(implementation = CollectCallRefundTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> refundTransaction(@RequestBody CollectCallRefundTransactionRequest refundRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "COLLECT_CALL_REFUND",
                    refundRequest.getTransactionId(), refundRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                refundRequest,
                "merchantId", "originalTransactionId", "amount", "merchantOrderId", "message", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = collectService.refundTransaction(refundRequest);

            StructuredLogger.Patterns.logApiResponse(log, "COLLECT_CALL_REFUND",
                    refundRequest.getTransactionId(), response.getBody(),
                    System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "COLLECT_CALL_REFUND",
                    refundRequest.getTransactionId(), "Error processing Collect Call refund", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }
}