package com.phonepe.payment.dqr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DqrCancelRequest {
    private Long id;
    private String reason;
    private String transactionId;
}
