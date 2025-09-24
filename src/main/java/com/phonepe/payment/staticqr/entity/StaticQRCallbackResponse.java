package com.phonepe.payment.staticqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "static_qr_callback_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "StaticQRCallbackResponse", description = "Response payload for Static QR callback")
public class StaticQRCallbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code", example = "PAYMENT_SUCCESS")
    private String code;

    @Schema(description = "Response message", example = "Your request has been successfully completed.")
    private String message;

    @Embedded
    @Schema(description = "Response data containing transaction details")
    private CallbackResponseData data;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CallbackResponseData", description = "Callback response data")
    public static class CallbackResponseData {

        @Schema(description = "Transaction identifier", example = "TXSCAN2102231651587876998585")
        private String transactionId;

        @Schema(description = "Merchant identifier", example = "IVEPOSUAT")
        private String merchantId;

        @Schema(description = "Provider reference identifier", example = "T2102231652258076998155")
        private String providerReferenceId;

        @Schema(description = "Transaction amount", example = "100")
        private int amount;

        @Schema(description = "Payment state", example = "PAYMENT_SUCCESS")
        private String paymentState;

        @Schema(description = "Payment response code", example = "SUCCESS")
        private String payResponseCode;

        @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JoinColumn(name = "callback_response_id")
        @Schema(description = "Payment modes used")
        private List<CallbackPaymentMode> paymentModes;

        @ElementCollection
        @CollectionTable(name = "static_qr_callback_transaction_context", joinColumns = @JoinColumn(name = "response_id"))
        @MapKeyColumn(name = "context_key")
        @Column(name = "context_value")
        @Schema(description = "Transaction context key-value pairs")
        private Map<String, String> transactionContext;
    }

    @Entity
    @Table(name = "static_qr_callback_payment_mode")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "CallbackPaymentMode", description = "Payment mode details for callback")
    public static class CallbackPaymentMode {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Schema(hidden = true)
        private Long id;

        @Schema(description = "Payment mode", example = "WALLET")
        private String mode;

        @Schema(description = "Amount for this payment mode", example = "100")
        private int amount;

        @Schema(description = "UTR (Unique Transaction Reference)", example = "123456789")
        private String utr;
    }
}

