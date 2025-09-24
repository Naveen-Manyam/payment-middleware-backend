package com.phonepe.payment.dqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dqr_transactions_status_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DqrCheckTransactionStatusRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // internal DB id, not exposed in API docs
    private Long id;

    @Schema(description = "Unique transaction identifier to check status for", example = "TXN123456789")
    private String transactionId;

    @Schema(description = "Merchant identifier associated with the transaction", example = "M12345")
    private String merchantId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Timestamp when the transaction status was checked (system-generated)", example = "2025-09-04T16:45:00")
    private LocalDateTime checkedStatusAt;
}
