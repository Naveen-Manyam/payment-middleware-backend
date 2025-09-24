package com.phonepe.payment.dqr.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dqr_cancel_transactions_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DqrCancelTransactionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true) // Internal DB field, not shown in API docs
    private Long id;

    @Schema(description = "Indicates whether the cancellation was successful", example = "true")
    private boolean success;

    @Schema(description = "Status/error code returned by the API", example = "CANCELLED")
    private String code;

    @Schema(description = "Detailed message about the cancellation result", example = "Transaction cancelled successfully")
    private String message;
}
