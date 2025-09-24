package com.phonepe.payment.paymentlink.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="payment_link_request")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "PaymentRequest", description = "Request payload for creating a payment link")
public class PaymentLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTUAT")
    private String merchantId;

    @Schema(description = "Unique transaction identifier", example = "TEST20231004021536")
    private String transactionId;

    @Schema(description = "Merchant's internal order identifier", example = "TEST20231004021536")
    private String merchantOrderId;

    @Schema(description = "Transaction amount in INR", example = "100")
    private Integer amount;

    @Schema(description = "Customer's mobile number", example = "8296412345")
    private String mobileNumber;

    @Schema(description = "Message or description for the payment", example = "paylink for 1 order")
    private String message;

    @Schema(description = "Payment link expiration time in seconds", example = "3600")
    private Integer expiresIn;

    @Schema(description = "Store identifier where transaction is initiated", example = "store1")
    private String storeId;

    @Schema(description = "Terminal identifier used for payment", example = "terminal1")
    private String terminalId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "Short name of the customer", example = "DemoCustomer")
    private String shortName;

    @Schema(description = "Sub-merchant identifier", example = "DemoMerchant")
    private String subMerchantId;

    @Schema(description = "Timestamp when the transaction was created",example = "2025-09-04T14:45:00")
    private LocalDateTime createdAt;
}

