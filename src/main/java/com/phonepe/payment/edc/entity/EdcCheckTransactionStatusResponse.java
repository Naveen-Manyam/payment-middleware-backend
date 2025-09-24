package com.phonepe.payment.edc.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "edc_transactions_status_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdcCheckTransactionStatusResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Whether the transaction status check was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code from PhonePe", example = "SUCCESS")
    private String code;

    @Schema(description = "Detailed response message", example = "Your request has been successfully completed.")
    private String message;

    @Embedded
    private PaymentData data;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {

        @Schema(description = "Unique identifier for the merchant", example = "FINXBRIDGEUAT")
        private String merchantId;

        @Schema(description = "Store identifier where transaction is initiated", example = "teststore1")
        private String storeId;

        @Schema(description = "Terminal identifier used for payment", example = "testterminal1")
        private String terminalId;

        @Schema(description = "Order identifier", example = "TX31266464521190")
        private String orderId;

        @Schema(description = "Unique transaction identifier", example = "TX31266464521190")
        private String transactionId;

        @Schema(description = "Reference number from payment gateway", example = "652449937182")
        private String referenceNumber;

        @Schema(description = "Payment mode used", example = "CARD")
        private String paymentMode;

        @Schema(description = "Transaction amount in paise", example = "10000")
        private Long amount;

        @Schema(description = "Transaction status", example = "SUCCESS")
        private String status;

        @Schema(description = "Response code from payment gateway", example = "null")
        private String responseCode;

        @Schema(description = "Timestamp of transaction", example = "1758608784641")
        private Long timestamp;

        @Transient
        @Schema(description = "Payment instruments used in the transaction")
        private List<PaymentInstrument> paymentInstruments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInstrument {

        @Schema(description = "Type of payment instrument", example = "CARD")
        private String type;

        @Column(name="response_amount", nullable = true)
        @Schema(description = "Amount paid through this instrument in paise", example = "10000")
        private Long amount;

        @Schema(description = "Last 4 digits of card", example = "5778")
        private String last4Digits;

        @Schema(description = "Card network", example = "VISA")
        private String cardNetwork;

        @Schema(description = "Card type", example = "DEBIT_CARD")
        private String cardType;
    }
}