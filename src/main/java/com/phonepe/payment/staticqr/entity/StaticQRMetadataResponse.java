package com.phonepe.payment.staticqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Entity
@Table(name = "static_qr_metadata_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "StaticQRMetadataResponse", description = "Response payload for Static QR transaction metadata")
public class StaticQRMetadataResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code", example = "SUCCESS")
    private String code;

    @Schema(description = "Response message", example = "Your request has been successfully completed.")
    private String message;

    @Embedded
    @Schema(description = "Response data containing transaction metadata")
    private MetadataResponseData data;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MetadataResponseData", description = "Metadata response data")
    public static class MetadataResponseData {

        @Schema(description = "Merchant identifier", example = "IVEPOSUAT")
        private String merchantId;

        @Schema(description = "PhonePe transaction identifier", example = "T2305311324226915327369")
        private String phonepeTransactionId;

        @Schema(description = "Schema version number", example = "IVEPOSUATV1")
        private String schemaVersionNumber;

        @ElementCollection
        @CollectionTable(name = "static_qr_metadata", joinColumns = @JoinColumn(name = "response_id"))
        @MapKeyColumn(name = "metadata_key")
        @Column(name = "metadata_value")
        @Schema(description = "Metadata key-value pairs", example = "{\"BILLNUMBER\": \"12381\"}")
        private Map<String, String> metadata;

        @Schema(description = "Number of metadata results returned", example = "1")
        private Integer resultCount;
    }
}
