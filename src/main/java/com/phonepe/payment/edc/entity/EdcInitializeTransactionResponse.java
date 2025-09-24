package com.phonepe.payment.edc.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "edc_initialize_transaction_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdcInitializeTransactionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean success;
    private String code;
    private String message;

    @Embedded
    private DataDto data;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataDto {
        private String merchantId;
        private String transactionId;
        private Long amount;
    }
}
