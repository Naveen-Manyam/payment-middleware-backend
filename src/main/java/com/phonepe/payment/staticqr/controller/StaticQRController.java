package com.phonepe.payment.staticqr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.staticqr.entity.*;
import com.phonepe.payment.staticqr.service.StaticQRService;
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
 * REST Controller for Static QR code payment transaction operations.
 *
 * <p>This controller provides RESTful endpoints for managing Static QR code transactions
 * through PhonePe APIs. Static QR codes are permanent QR codes that can be used for
 * multiple transactions without regeneration.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see StaticQRService
 */
@RestController
@RequestMapping("/api/phonepe/static-qr")
@Slf4j
public class StaticQRController {

    private final StaticQRService staticQRService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public StaticQRController(StaticQRService staticQRService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.staticQRService = staticQRService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    @PostMapping("/transaction/list")
    @Operation(summary = "transaction list", description = "Generates transaction list based on size.")
    @ApiResponse(responseCode = "200", description = "Transaction list generated successfully", content = @Content(schema = @Schema(implementation = StaticQRTransactionListResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> initTransaction(@RequestBody StaticQRTransactionListRequest staticQRTransactionListRequest) {
        long startTime = System.currentTimeMillis();
        String requestId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_TRANSACTION_LIST")
                .requestId(requestId)
                .field("requestPayload", staticQRTransactionListRequest)
                .timestamp()
                .info("Static QR transaction list request received");
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                staticQRTransactionListRequest,
                "merchantId", "provider", "size", "storeId"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = staticQRService.getTransactionList(staticQRTransactionListRequest);

            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_TRANSACTION_LIST")
                .requestId(requestId)
                .field("responsePayload", response.getBody())
                .responseTime(System.currentTimeMillis() - startTime)
                .httpStatus(response.getStatusCode().value())
                .timestamp()
                .info("Static QR transaction list response sent");

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_TRANSACTION_LIST")
                .requestId(requestId)
                .error(e)
                .timestamp()
                .error("Error getting static QR transaction list", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }



    @PostMapping("/transaction/metadata")
    @Operation(summary = "transaction metadata", description = "Generates transaction metadata.")
    @ApiResponse(responseCode = "200", description = "Transaction metadata generated successfully", content = @Content(schema = @Schema(implementation = StaticQRMetadataResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> getTransactionMetadata(@RequestBody StaticQRMetadataRequest staticQRMetadataRequest) {
        long startTime = System.currentTimeMillis();
        String requestId = StructuredLogger.generateRequestId();

        try {
            StructuredLogger.forLogger(log)
                    .operation("STATIC_QR_TRANSACTION_METADATA")
                    .requestId(requestId)
                    .field("requestPayload", staticQRMetadataRequest)
                    .timestamp()
                    .info("Static QR transaction metadata request received");
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                staticQRMetadataRequest,
                "merchantId", "provider", "phonepeTransactionId", "schemaVersionNumber"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = staticQRService.getTransactionMetadata(staticQRMetadataRequest);

            StructuredLogger.forLogger(log)
                    .operation("STATIC_QR_TRANSACTION_METADATA")
                    .requestId(requestId)
                    .field("responsePayload", response.getBody())
                    .responseTime(System.currentTimeMillis() - startTime)
                    .httpStatus(response.getStatusCode().value())
                    .timestamp()
                    .info("Static QR transaction metadata response sent");

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("STATIC_QR_TRANSACTION_METADATA")
                    .requestId(requestId)
                    .error(e)
                    .timestamp()
                    .error("Error getting static QR transaction metadata", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

}
