package com.phonepe.payment.dqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dqr_cancel_transactions_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DqrCancelTransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Internal DB ID, not exposed in API
    private Long id;

    @Schema(description = "Unique identifier for the transaction to be cancelled", example = "TXN1234567890")
    private String transactionId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Merchant identifier registered with PhonePe", example = "MERCHANT98765")
    private String merchantId;

    @Schema(description = "Reason for cancelling the transaction", example = "Customer requested cancellation")
    private String reason;

    @Schema(description = "Timestamp when the transaction was cancelled", example = "2025-09-04T16:35:00")
    private LocalDateTime cancelledAt;
}
