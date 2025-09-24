package com.phonepe.payment.staticqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="static_qr_transaction_list_request")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "StaticQRTransactionListRequest", description = "Request payload for creating a transaction list")
public class StaticQRTransactionListRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Page size or number of records to fetch", example = "10")
    private Integer size;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTSQRUAT")
    private String merchantId;

    @Schema(description = "Store identifier where transaction is initiated", example = "teststore1")
    private String storeId;

    @Schema(description = "Transaction amount in INR", example = "200")
    private Long amount;

    @Schema(description = "Timestamp when the transaction was created",example = "2025-09-04T14:45:00")
    private LocalDateTime createdAt;

    private Long startTimestamp;
}

