package com.phonepe.payment.edc.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "edc_initialize_transaction_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdcInitializeTransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Auto-generated, not required in API input
    private Long id;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTUAT")
    private String merchantId;

    @Schema(description = "Store identifier where transaction is initiated", example = "store1")
    private String storeId;

    @Schema(description = "Unique transaction ID generated for this request", example = "order1")
    private String orderId;

    @Schema(description = "Unique transaction ID generated for this request",example = "TXN1234567890")
    private String transactionId;

    @Schema(description = "Merchant-provided order ID",example = "ORD987654321")
    private String merchantOrderId;

    @Schema(description = "Terminal identifier used for payment", example = "terminal1")
    private String terminalId;

    @Schema(description = "Type of integration used for payment", example = "ONE_TO_ONE")
    private String integrationMappingType;

    @Schema(description = "Type of integration used for payment", example = "CARD/DQR")
    private List<String> paymentModes;

    @Schema(description = "Transaction amount in INR paisa (e.g., 10000 = ₹100)",example = "10000")
    private Long amount;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Time allowed for handover in seconds",example = "60")
    private Integer timeAllowedForHandoverToTerminalSeconds;

    @Schema(description = "Timestamp when the transaction was created",example = "2025-09-04T14:45:00")
    private LocalDateTime createdAt;
}
