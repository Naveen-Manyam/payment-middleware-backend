package com.phonepe.payment.collect.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a PhonePe Collect Call response.
 *
 * <p>This entity stores the response details from PhonePe collect call API,
 * including transaction status, generated transaction ID, and merchant information
 * returned after initiating a collect call transaction.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectCallRequest
 */
@Entity
@Table(name = "collect_call_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CollectCallResponse", description = "Response payload for PhonePe collect call transaction")
public class CollectCallResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Whether the request was successful", example = "true")
    private Boolean success;

    @Schema(description = "Response code", example = "SUCCESS")
    private String code;

    @Schema(description = "Response message", example = "Your request has been successfully completed.")
    private String message;

    @Embedded
    @Schema(description = "Response data containing transaction details")
    private CollectCallResponseData data;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "CollectCallResponseData", description = "Collect call response data")
    public static class CollectCallResponseData {

        @Schema(description = "Generated transaction identifier", example = "55fe801b-1092-461a-8480-c80d9781498a")
        private String transactionId;

        @Schema(description = "Transaction amount", example = "300")
        private Integer amount;

        @Schema(description = "Merchant identifier", example = "MERCHANTUAT")
        private String merchantId;

        @Schema(description = "Provider Reference identifier", example = "TX30990300163756")
        private String providerReferenceId;


    }
}