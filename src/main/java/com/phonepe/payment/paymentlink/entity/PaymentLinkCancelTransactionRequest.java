package com.phonepe.payment.paymentlink.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a request to cancel a PaymentLink transaction.
 *
 * <p>This entity captures the details required to cancel an existing payment link
 * transaction before the customer completes the payment. Cancellation is typically
 * initiated by the merchant for various business reasons.
 *
 * <p>Key Properties:
 * <ul>
 *   <li><b>transactionId</b> - The unique identifier of the transaction to cancel</li>
 *   <li><b>merchantId</b> - The merchant identifier registered with PhonePe</li>
 *   <li><b>reason</b> - Business reason for the cancellation</li>
 *   <li><b>provider</b> - Payment provider (typically "PHONEPE")</li>
 * </ul>
 *
 * <p>Database Mapping:
 * Stored in table {@code paymentlink_cancel_transactions_request} with auto-generated
 * primary key and timestamp tracking for audit purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCancelTransactionResponse
 */
@Entity
@Table(name = "paymentlink_cancel_transactions_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "PaymentLinkCancelTransactionRequest", description = "Request payload for cancelling a PaymentLink transaction")
public class PaymentLinkCancelTransactionRequest {

    /**
     * Auto-generated primary key for database storage.
     * This field is not exposed in API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    /**
     * Unique identifier for the payment link transaction to be cancelled.
     * Must match an existing transaction that is in a cancellable state.
     */
    @Schema(description = "Unique identifier for the transaction to be cancelled",
            example = "TXLINK1234567890",
            required = true)
    @Column(nullable = false)
    private String transactionId;

    /**
     * Payment provider identifier, typically "PHONEPE".
     */
    @Schema(description = "Payment provider identifier",
            example = "PHONEPE",
            required = true)
    @Column(nullable = false)
    private String provider;

    /**
     * Merchant identifier registered with PhonePe.
     * Used to validate merchant ownership of the transaction.
     */
    @Schema(description = "Merchant identifier registered with PhonePe",
            example = "MERCHANT98765",
            required = true)
    @Column(nullable = false)
    private String merchantId;

    /**
     * Business reason for cancelling the transaction.
     * Helps in tracking and analytics of cancellation patterns.
     */
    @Schema(description = "Reason for cancelling the transaction",
            example = "Customer requested cancellation")
    private String reason;

    /**
     * Timestamp when the cancellation request was created.
     * Automatically set when the entity is persisted.
     */
    @Schema(description = "Timestamp when the transaction was cancelled",
            example = "2024-01-15T16:35:00")
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the created timestamp to current time before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}