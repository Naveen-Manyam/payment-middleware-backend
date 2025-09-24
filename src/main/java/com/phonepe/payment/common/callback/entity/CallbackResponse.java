package com.phonepe.payment.common.callback.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "phonepe_callback_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CallbackResponse", description = "Response payload for PhonePe S2S callback")
public class CallbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Whether the request was successful", example = "true")
    private Boolean success;

    @Schema(description = "Response code", example = "PAYMENT_SUCCESS")
    private String code;

    @Schema(description = "Response message", example = "Your request has been successfully completed.")
    private String message;

    @Schema(description = "Transaction identifier", example = "TXSCAN2102231651587876998585")
    private String transactionId;

    @Schema(description = "Merchant identifier", example = "IVEPOSUAT")
    private String merchantId;

    @Schema(description = "Provider reference identifier", example = "T2102231652258076998155")
    private String providerReferenceId;

    @Schema(description = "Transaction amount", example = "100")
    private Integer amount;

    @Schema(description = "Payment state", example = "PAYMENT_SUCCESS")
    private String paymentState;

    @Schema(description = "Payment response code", example = "SUCCESS")
    private String payResponseCode;

    @Schema(description = "Mobile number (masked)", example = "94XXXXX987")
    private String mobileNumber;

    @Schema(description = "Phone number (masked)", example = "94XXXXX987")
    private String phoneNumber;

    @Schema(description = "Customer name", example = "Shankhajyoti")
    private String customerName;

    @ElementCollection
    @CollectionTable(name = "phonepe_callback_transaction_context", joinColumns = @JoinColumn(name = "response_id"))
    @MapKeyColumn(name = "context_key")
    @Column(name = "context_value")
    @Schema(description = "Transaction context key-value pairs")
    private Map<String, String> transactionContext;

    @ElementCollection
    @CollectionTable(name = "phonepe_callback_payment_modes", joinColumns = @JoinColumn(name = "response_id"))
    @MapKeyColumn(name = "mode_type")
    @Column(name = "mode_details", columnDefinition = "TEXT")
    @Schema(description = "Payment modes used")
    private Map<String, String> paymentModes;

    @Schema(description = "Response processed timestamp")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Schema(description = "Raw JSON response data", example = "{\"data\": {...}}")
    @Column(columnDefinition = "TEXT")
    private String rawJsonData;
}