package com.phonepe.payment.staticqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "static_qr_metadata_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "StaticQRMetadataRequest", description = "Request payload for Static QR transaction metadata")
public class StaticQRMetadataRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Unique identifier for the merchant", example = "MERCHANTSQRUAT", required = true)
    private String merchantId;

    @Schema(description = "Unique merchant identifier", example = "MERCHANTPROVIDER")
    private String provider;

    @Schema(description = "PhonePe transaction identifier", example = "T2305311324226915327369", required = true)
    private String phonepeTransactionId;

    @Schema(description = "Schema version number", example = "MERCHANTSQRUATV1", required = true)
    private String schemaVersionNumber;

    @ElementCollection
    @CollectionTable(name = "static_qr_metadata_request_metadata", joinColumns = @JoinColumn(name = "request_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @Schema(description = "Metadata key-value pairs", example = "{\"BILLNUMBER\": \"12381\"}")
    private Map<String, String> metadata;

    @Schema(description = "Timestamp when the request was created", hidden = true)
    private LocalDateTime createdAt;
}