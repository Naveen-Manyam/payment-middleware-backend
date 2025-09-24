package com.phonepe.payment.dqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dqr_initialize_transaction_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DqrInitializeTransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Auto-generated, not required in API input
    private Long id;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTUAT")
    private String merchantId;

    @Schema(description = "Unique transaction ID generated for this request",example = "TXN1234567890")
    private String transactionId;

    @Schema(description = "Merchant-provided order ID",example = "ORD987654321")
    private String merchantOrderId;

    @Schema(description = "Transaction amount in INR paisa (e.g., 10000 = ₹100)",example = "10000")
    private Long amount;

    @Schema(description = "Payment link expiration time in seconds", example = "3600")
    private Integer expiresIn;

    @Schema(description = "Store identifier where transaction is initiated", example = "store1")
    private String storeId;

    @Schema(description = "Terminal identifier used for payment", example = "terminal1")
    private String terminalId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Timestamp when the transaction was created",example = "2025-09-04T14:45:00")
    private LocalDateTime createdAt;
}
