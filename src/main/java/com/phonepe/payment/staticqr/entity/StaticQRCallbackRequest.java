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
@Table(name = "static_qr_callback_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "StaticQRCallbackRequest", description = "Request payload for Static QR transaction metadata")
public class StaticQRCallbackRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "An base 64 encoded response", example = "eyJzdWNjZXNzIjp0cnVlLCJjb2RlIjoiUEFZTUVOVF9TVUNDRVNTIiwib", required = true)
    private String response;
}
