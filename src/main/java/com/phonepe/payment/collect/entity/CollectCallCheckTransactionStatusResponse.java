package com.phonepe.payment.collect.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the response from a CollectCall transaction status check request.
 *
 * <p>This entity captures the comprehensive status information returned by PhonePe API
 * when checking the current state of a payment link transaction. It includes transaction
 * details, payment state, and instrument information.
 *
 * <p>Response Structure:
 * <ul>
 *   <li><b>success</b> - Boolean indicating if the status check was successful</li>
 *   <li><b>code</b> - Response code from PhonePe API</li>
 *   <li><b>message</b> - Descriptive message about the status check result</li>
 *   <li><b>data</b> - Embedded payment data with transaction details</li>
 * </ul>
 *
 * <p>Payment States:
 * <ul>
 *   <li><b>PENDING</b> - Payment link created, waiting for customer action</li>
 *   <li><b>COMPLETED</b> - Payment successfully completed</li>
 *   <li><b>FAILED</b> - Payment attempt failed</li>
 *   <li><b>CANCELLED</b> - Transaction cancelled by merchant or customer</li>
 *   <li><b>EXPIRED</b> - Payment link expired without completion</li>
 * </ul>
 *
 * <p>Database Mapping:
 * Stored in table {@code CollectCall_transactions_status_response} with
 * embedded payment data and instrument details.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see CollectCallCheckTransactionStatusRequest
 */
@Entity
@Table(name = "collect_call_transactions_status_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CollectCallCheckTransactionStatusResponse", description = "Response payload for CollectCall transaction status check")
public class CollectCallCheckTransactionStatusResponse {

    /**
     * Auto-generated primary key for database storage.
     * This field is not exposed in API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    /**
     * Indicates whether the transaction status check was successful.
     * True if status was retrieved successfully, false if there was an error.
     */
    @Schema(description = "Whether the transaction status check was successful",
            example = "true",
            required = true)
    @Column(nullable = false)
    private boolean success;

    /**
     * Response code returned by PhonePe API.
     * Indicates the result of the status check operation.
     */
    @Schema(description = "Response code from PhonePe",
            example = "SUCCESS",
            required = true)
    @Column(nullable = false)
    private String code;

    /**
     * Detailed human-readable message about the status check result.
     * Provides additional context about the transaction state.
     */
    @Schema(description = "Detailed response message",
            example = "Transaction found successfully")
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Embedded payment data containing transaction details and status information.
     */
    @Embedded
    @Schema(description = "Payment data containing transaction details")
    private PaymentData data;

    /**
     * Embedded class containing payment transaction details.
     *
     * <p>This class encapsulates all the essential information about
     * the payment transaction including identifiers, amount, state,
     * and payment instrument details.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PaymentData", description = "Payment transaction details")
    public static class PaymentData {

        /**
         * Unique identifier for the merchant who initiated the transaction.
         */
        @Schema(description = "Unique identifier for the merchant",
                example = "MERCHANT12345")
        private String merchantId;

        /**
         * Unique transaction identifier for the payment link transaction.
         */
        @Schema(description = "Unique transaction identifier",
                example = "TXLINK123456789")
        private String transactionId;

        /**
         * Transaction amount in paise (1 rupee = 100 paise).
         */
        @Schema(description = "Transaction amount in paise",
                example = "10000")
        private Long amount;

        /**
         * Current state of the payment transaction.
         * Possible values: PENDING, COMPLETED, FAILED, CANCELLED, EXPIRED.
         */
        @Schema(description = "Current state of the transaction", example = "COMPLETED")
        private String paymentState;

        /**
         * Embedded payment instrument information.
         * Contains details about how the payment was processed.
         */
        @Schema(description = "Payment instrument details")
        private String paymentInstrument;
    }
}