package com.phonepe.payment.common.callback.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "phonepe_callback_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "CallbackRequest", description = "Request payload for PhonePe S2S callback")
public class CallbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Base64 encoded response from PhonePe", example = "eyJzdWNjZXNzIjp0cnVlLCJjb2RlIjoiUEFZTUVOVF9TVUNDRVNTIiwib", required = true)
    @Column(columnDefinition = "TEXT")
    private String response;

    @Schema(description = "X-VERIFY header value", example = "hash###1")
    private String xVerifyHeader;

    @Schema(description = "Request received timestamp")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Schema(description = "Whether X-VERIFY validation passed", example = "true")
    private Boolean xVerifyValid;

    @Schema(description = "Error message if processing failed")
    private String errorMessage;
}