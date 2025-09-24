package com.phonepe.payment.edc.controller;

import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusRequest;
import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusResponse;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionRequest;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionResponse;
import com.phonepe.payment.edc.service.EdcService;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.util.CommonServiceUtils;
import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.exception.TrackExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Electronic Data Capture (EDC) payment transaction operations.
 *
 * <p>This controller provides RESTful endpoints for managing EDC-based payment transactions
 * through PhonePe APIs. It serves as the HTTP interface layer between client applications
 * and the underlying EDC service layer for point-of-sale terminal integrations.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see EdcService
 */
@RestController
@RequestMapping("/api/phonepe/edc")
@Slf4j
public class EdcController {

    private final EdcService edcService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;

    @Autowired
    public EdcController(EdcService edcService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.edcService = edcService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    @PostMapping("/init")
    @Operation(summary = "Initialize Payment QR", description = "Generates a dynamic QR for UPI payment using PhonePe by passing an amount.")
    @ApiResponse(responseCode = "200", description = "QR generated successfully", content = @Content(schema = @Schema(implementation = EdcInitializeTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> initTransaction(@RequestBody EdcInitializeTransactionRequest edcInitializeTransactionRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "EDC_INIT",
                edcInitializeTransactionRequest.getTransactionId(), edcInitializeTransactionRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                edcInitializeTransactionRequest,
                "merchantId", "storeId", "orderId", "terminalId", "amount", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = edcService.initTransaction(edcInitializeTransactionRequest);

            StructuredLogger.Patterns.logApiResponse(log, "EDC_INIT",
                edcInitializeTransactionRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "EDC_INIT",
                edcInitializeTransactionRequest.getTransactionId(), "Error initializing EDC transaction", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/status")
    @Operation(summary = "Check Payment Status", description = "Fetches the current status of a transaction by transaction ID.")
    @ApiResponse(responseCode = "200", description = "Transaction status retrieved successfully", content = @Content(schema = @Schema(implementation = EdcCheckTransactionStatusResponse.class)))
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentStatus(@RequestBody EdcCheckTransactionStatusRequest edcCheckTransactionStatusRequest) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "EDC_STATUS_CHECK",
                edcCheckTransactionStatusRequest.getTransactionId(), edcCheckTransactionStatusRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                edcCheckTransactionStatusRequest,
                "merchantId", "transactionId", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = edcService.checkPaymentStatus(edcCheckTransactionStatusRequest);

            StructuredLogger.Patterns.logApiResponse(log, "EDC_STATUS_CHECK",
                edcCheckTransactionStatusRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "EDC_STATUS_CHECK",
                edcCheckTransactionStatusRequest.getTransactionId(), "Error checking EDC payment status", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }
}
