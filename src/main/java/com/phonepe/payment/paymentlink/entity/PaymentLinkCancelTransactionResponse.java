package com.phonepe.payment.paymentlink.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the response from a PaymentLink transaction cancellation request.
 *
 * <p>This entity captures the response received from PhonePe API when attempting
 * to cancel a payment link transaction. It contains the operation result status,
 * response codes, and descriptive messages.
 *
 * <p>Response Structure:
 * <ul>
 *   <li><b>success</b> - Boolean indicating if the cancellation was successful</li>
 *   <li><b>code</b> - Status or error code returned by PhonePe API</li>
 *   <li><b>message</b> - Human-readable message describing the result</li>
 * </ul>
 *
 * <p>Common Response Codes:
 * <ul>
 *   <li><b>SUCCESS</b> - Transaction cancelled successfully</li>
 *   <li><b>CANCELLED</b> - Transaction already cancelled</li>
 *   <li><b>PAYMENT_COMPLETED</b> - Cannot cancel, payment already completed</li>
 *   <li><b>INVALID_TRANSACTION</b> - Transaction not found or invalid</li>
 * </ul>
 *
 * <p>Database Mapping:
 * Stored in table {@code paymentlink_cancel_transactions_response} for audit
 * and troubleshooting purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCancelTransactionRequest
 */
@Entity
@Table(name = "paymentlink_cancel_transactions_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "PaymentLinkCancelTransactionResponse", description = "Response payload for PaymentLink transaction cancellation")
public class PaymentLinkCancelTransactionResponse {

    /**
     * Auto-generated primary key for database storage.
     * This field is not exposed in API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    /**
     * Indicates whether the cancellation request was successful.
     * True if the transaction was cancelled successfully, false otherwise.
     */
    @Schema(description = "Indicates whether the cancellation was successful",
            example = "true",
            required = true)
    @Column(nullable = false)
    private boolean success;

    /**
     * Status or error code returned by the PhonePe API.
     * Provides specific information about the cancellation result.
     */
    @Schema(description = "Status/error code returned by the API",
            example = "SUCCESS",
            required = true)
    @Column(nullable = false)
    private String code;

    /**
     * Detailed human-readable message about the cancellation result.
     * Provides additional context about the success or failure reason.
     */
    @Schema(description = "Detailed message about the cancellation result",
            example = "Transaction cancelled successfully")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;
}