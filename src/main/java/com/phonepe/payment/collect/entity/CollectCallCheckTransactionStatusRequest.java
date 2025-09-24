package com.phonepe.payment.collect.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a request to check the status of a CollectCall transaction.
 *
 * <p>This entity captures the details required to query the current status of
 * a payment link transaction. Status checks are typically performed to track
 * payment progress and handle transaction lifecycle events.
 *
 * <p>Key Properties:
 * <ul>
 *   <li><b>transactionId</b> - The unique identifier of the transaction to check</li>
 *   <li><b>merchantId</b> - The merchant identifier for validation</li>
 *   <li><b>provider</b> - Payment provider (typically "PHONEPE")</li>
 *   <li><b>checkedStatusAt</b> - Timestamp when the status check was performed</li>
 * </ul>
 *
 * <p>Common Use Cases:
 * <ul>
 *   <li>Periodic status polling for payment completion</li>
 *   <li>Webhook validation and confirmation</li>
 *   <li>Customer service inquiry support</li>
 *   <li>Payment reconciliation processes</li>
 * </ul>
 *
 * <p>Database Mapping:
 * Stored in table {@code CollectCall_transactions_status_request} with
 * automatic timestamp tracking for audit purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see CollectCallCheckTransactionStatusResponse
 */
@Entity
@Table(name = "collect_call_transactions_status_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CollectCallCheckTransactionStatusRequest", description = "Request payload for checking CollectCall transaction status")
public class CollectCallCheckTransactionStatusRequest {

    /**
     * Auto-generated primary key for database storage.
     * This field is not exposed in API responses.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    /**
     * Unique identifier for the payment link transaction to check status for.
     * Must match an existing transaction in the system.
     */
    @Schema(description = "Unique transaction identifier to check status for",
            example = "TXLINK123456789",
            required = true)
    @Column(nullable = false)
    private String transactionId;

    /**
     * Merchant identifier associated with the transaction.
     * Used for validation and authorization of the status check request.
     */
    @Schema(description = "Merchant identifier associated with the transaction",
            example = "MERCHANT12345",
            required = true)
    @Column(nullable = false)
    private String merchantId;

    /**
     * Payment provider identifier, typically "PHONEPE".
     */
    @Schema(description = "Payment provider identifier",
            example = "PHONEPE",
            required = true)
    @Column(nullable = false)
    private String provider;

    /**
     * Timestamp when the transaction status was checked.
     * Automatically set when the status check request is created.
     */
    @Schema(description = "Timestamp when the transaction status was checked (system-generated)",
            example = "2024-01-15T16:45:00")
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the status check timestamp to current time before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}