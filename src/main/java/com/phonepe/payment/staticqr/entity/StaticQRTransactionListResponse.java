package com.phonepe.payment.staticqr.entity;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "static_qr_transaction_list_response")
@Schema(description = "Static QR Transaction List API Response")
public class StaticQRTransactionListResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code from PhonePe", example = "SUCCESS")
    private String code;

    @Schema(description = "Human-readable message", example = "Your request has been successfully completed.")
    private String message;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_history_id", referencedColumnName = "id")
    private TransactionHistoryData data;

    // ------------------ CHILD ENTITIES ------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Entity
    @Table(name = "transaction_history_data")
    @Schema(description = "Transaction list metadata and transactions")
    public static class TransactionHistoryData {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Schema(hidden = true)
        private Long id;

        @Schema(description = "Number of transactions returned", example = "1")
        private Integer resultCount;

        @Schema(description = "Start timestamp of search window (epoch ms)", example = "1614079349293")
        private Long startTimestamp;

        @Schema(description = "End timestamp of search window (epoch ms)", example = "1614079349293")
        private Long endTimestamp;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "transaction_history_id") // foreign key in transaction table
        @ArraySchema(schema = @Schema(description = "List of transactions"))
        private List<Transaction> transactions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Entity
    @Table(name = "transactions")
    @Schema(description = "Transaction details")
    public static class Transaction {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Schema(hidden = true)
        private Long id;

        @Schema(description = "Unique transaction ID", example = "TXSCAN2102231651587876998585")
        private String transactionId;

        @Schema(description = "Reference ID from the provider", example = "T210223165225807")
        private String providerReferenceId;

        @Schema(description = "Transaction amount in paise", example = "20000")
        private Long amount;

        @Schema(description = "Payment state", example = "COMPLETED")
        private String paymentState;

        @Schema(description = "Transaction date and time", example = "2025-09-15T09:30:00Z")
        private String transactionDate;

        @Schema(description = "Payment response code", example = "SUCCESS")
        private String payResponseCode;

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "transaction_id") // foreign key in payment_modes table
        @ArraySchema(schema = @Schema(description = "Payment modes used in this transaction"))
        private List<PaymentMode> paymentModes;

        @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "transaction_context_id")
        private TransactionContext transactionContext;

        @Schema(description = "Customer's mobile number", example = "9876543210")
        private String mobileNumber;

        @Schema(description = "Customer's phone number", example = "9876543210")
        private String phoneNumber;

        @Schema(description = "Customer name", example = "John Doe")
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Entity
    @Table(name = "payment_modes")
    @Schema(description = "Payment mode details")
    public static class PaymentMode {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Schema(hidden = true)
        private Long id;

        @Schema(description = "Type of payment mode", example = "UPI")
        private String type;

        @Schema(description = "Amount for this mode in paise", example = "20000")
        private Long amount;

        @Schema(description = "UTR for this payment", example = "1234567890")
        private String utr;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Entity
    @Table(name = "transaction_context")
    @Schema(description = "Context information of the transaction")
    public static class TransactionContext {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Schema(hidden = true)
        private Long id;

        @Schema(description = "QR code ID", example = "QR12345")
        private String qrCodeId;

        @Schema(description = "POS device ID", example = "POS98765")
        private String posDeviceId;

        @Schema(description = "Store ID", example = "STORE001")
        private String storeId;

        @Schema(description = "Terminal ID", example = "TERM001")
        private String terminalId;
    }
}
