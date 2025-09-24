package com.phonepe.payment.dqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dqr_transactions_status_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DqrCheckTransactionStatusResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Hide internal DB id
    private Long id;

    @Schema(description = "Whether the transaction status check was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code from PhonePe", example = "SUCCESS")
    private String code;

    @Schema(description = "Detailed response message", example = "Transaction found successfully")
    private String message;

    @Embedded
    private PaymentData data;

    @Embeddable
    @Data
    public static class PaymentData {

        @Schema(description = "Unique identifier for the merchant", example = "M12345")
        private String merchantId;

        @Schema(description = "Unique transaction identifier", example = "TXN123456789")
        private String transactionId;

        @Schema(description = "Transaction amount in paise", example = "10000")
        private Long amount;

        @Schema(description = "Current state of the transaction", example = "COMPLETED")
        private String paymentState;

        /**
         * Embedded payment instrument information.
         * Contains details about how the payment was processed.
         */
        @Schema(description = "Payment instrument details")
        private String paymentInstrument;
    }
}
