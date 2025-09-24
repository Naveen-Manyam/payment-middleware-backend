package com.phonepe.payment.paymentlink.controller;

import com.phonepe.payment.paymentlink.entity.PaymentLinkRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionResponse;
import com.phonepe.payment.paymentlink.service.PaymentLinkService;
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
 * REST Controller for Payment Link transaction operations.
 *
 * <p>This controller provides RESTful endpoints for managing Payment Link transactions
 * through PhonePe APIs. Payment Links allow customers to complete payments through
 * a web-based interface accessible via URL links.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkService
 */
@RestController
@RequestMapping("/api/phonepe/paymentLink")
@Slf4j
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;

    @Autowired
    public PaymentLinkController(PaymentLinkService paymentLinkService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.paymentLinkService = paymentLinkService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    @PostMapping("/getPaymentLink")
    @Operation(summary = "Initialize Payment QR", description = "Generates a dynamic QR for UPI payment using PhonePe by passing an amount.")
    @ApiResponse(responseCode = "200", description = "QR generated successfully", content = @Content(schema = @Schema(implementation = PaymentLinkResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> initTransaction(@RequestBody PaymentLinkRequest paymentLinkRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "PAYMENT_LINK_INIT",
                paymentLinkRequest.getMerchantId(), paymentLinkRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                paymentLinkRequest,
                "merchantId", "amount", "mobileNumber", "message", "expiresIn", "storeId", "terminalId", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = paymentLinkService.initTransaction(paymentLinkRequest);

            StructuredLogger.Patterns.logApiResponse(log, "PAYMENT_LINK_INIT",
                paymentLinkRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_INIT",
                paymentLinkRequest.getTransactionId(), "Error initializing payment link", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel Payment Link Transaction", description = "Cancels an active payment link transaction using PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Transaction cancelled successfully", content = @Content(schema = @Schema(implementation = PaymentLinkCancelTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> cancelTransaction(@RequestBody PaymentLinkCancelTransactionRequest cancelRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "PAYMENT_LINK_CANCEL",
                cancelRequest.getTransactionId(), cancelRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                cancelRequest,
                "merchantId", "transactionId", "provider", "reason"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = paymentLinkService.cancelTransaction(cancelRequest);

            StructuredLogger.Patterns.logApiResponse(log, "PAYMENT_LINK_CANCEL",
                cancelRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_CANCEL",
                cancelRequest.getTransactionId(), "Error cancelling payment link transaction", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/status")
    @Operation(summary = "Check Payment Link Transaction Status", description = "Retrieves the current status of a payment link transaction from PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully", content = @Content(schema = @Schema(implementation = PaymentLinkCheckTransactionStatusResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> checkPaymentStatus(@RequestBody PaymentLinkCheckTransactionStatusRequest statusRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "PAYMENT_LINK_STATUS_CHECK",
                statusRequest.getTransactionId(), statusRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequiredFields(
                statusRequest,
                "merchantId", "transactionId", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = paymentLinkService.checkPaymentStatus(statusRequest);

            StructuredLogger.Patterns.logApiResponse(log, "PAYMENT_LINK_STATUS_CHECK",
                statusRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_STATUS_CHECK",
                statusRequest.getTransactionId(), "Error checking payment link transaction status", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund Payment Link Transaction", description = "Processes a refund for a completed payment link transaction using PhonePe API.")
    @ApiResponse(responseCode = "200", description = "Refund processed successfully", content = @Content(schema = @Schema(implementation = PaymentLinkRefundTransactionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request (invalid input)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY mismatch)")
    @ApiResponse(responseCode = "404", description = "Transaction not found")
    @ApiResponse(responseCode = "500", description = "PhonePe server error")
    public ResponseEntity<?> refundTransaction(@RequestBody PaymentLinkRefundTransactionRequest refundRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.Patterns.logApiRequest(log, "PAYMENT_LINK_REFUND",
                refundRequest.getTransactionId(), refundRequest);
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                refundRequest,
                "merchantId", "originalTransactionId", "amount", "merchantOrderId", "message", "provider"
            );
            if (validationResult != null) {
                return validationResult;
            }

            ResponseEntity<?> response = paymentLinkService.refundTransaction(refundRequest);

            StructuredLogger.Patterns.logApiResponse(log, "PAYMENT_LINK_REFUND",
                refundRequest.getTransactionId(), response.getBody(),
                System.currentTimeMillis() - startTime, response.getStatusCode().value());

            return response;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_REFUND",
                refundRequest.getTransactionId(), "Error processing payment link refund", e);
            throw e;
        } finally {
            StructuredLogger.clearMDC();
        }
    }
}
