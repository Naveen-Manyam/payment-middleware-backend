package com.phonepe.payment.collect.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a PhonePe Collect Call request.
 *
 * <p>This entity stores the request details for PhonePe collect call transactions,
 * including merchant information, transaction details, and instrument configuration
 * for mobile-based payment collection.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectCallResponse
 */
@Entity
@Table(name = "collect_call_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CollectCallRequest", description = "Request payload for PhonePe collect call transaction")
public class CollectCallRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Merchant identifier", example = "MERCHANTUAT", required = true)
    @Column(nullable = false)
    private String merchantId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Transaction identifier", example = "TX123456789", required = true)
    @Column(nullable = false)
    private String transactionId;

    @Schema(description = "Merchant order identifier", example = "M123456789", required = true)
    @Column(nullable = false)
    private String merchantOrderId;

    @Schema(description = "Transaction amount in smallest currency unit", example = "100", required = true)
    @Column(nullable = false)
    private Integer amount;

    @Schema(description = "Instrument type for payment", example = "MOBILE", required = true)
    @Column(nullable = false)
    private String instrumentType;

    @Schema(description = "Instrument reference (mobile number)", example = "9999999999", required = true)
    @Column(nullable = false)
    private String instrumentReference;

    @Schema(description = "Payment message description", example = "collect for XXX order")
    private String message;

    @Schema(description = "Customer email address", example = "amitxxx75@gmail.com")
    private String email;

    @Schema(description = "Payment expiry time in seconds", example = "180")
    private Integer expiresIn;

    @Schema(description = "Customer short name", example = "DemoCustomer")
    private String shortName;

    @Schema(description = "Store identifier", example = "store1")
    private String storeId;

    @Schema(description = "Terminal identifier", example = "terminal1")
    private String terminalId;
}