package com.phonepe.payment.paymentlink.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name="payment_link_response")
@Schema(name = "PaymentResponse", description = "Response payload for payment request")
public class PaymentLinkResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(hidden = true)
    private Long id;

    @Schema(description = "Flag indicating whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response code for the request", example = "SUCCESS")
    private String code;

    @Schema(description = "Response message from the system", example = "Your request has been successfully completed.")
    private String message;

    @Schema(description = "Payment response data")
    private PaymentResponseData data;


    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PaymentResponseData", description = "Detailed payment response information")
    public static class PaymentResponseData {

        @Schema(description = "Unique transaction identifier", example = "TEST20231004021536")
        private String transactionId;

        @Schema(description = "Transaction amount in INR", example = "100")
        private Integer amount;

        @Schema(description = "Unique merchant identifier", example = "MERCHANTUAT")
        private String merchantId;

        @Schema(description = "Generated UPI intent for payment",
                example = "upi://pay?pa=MERCHANTUAT@ybl&pn=P2Mstore3&am=100&mam=100&tr=TEST20231004021536&tn=Payment%20for%20M123456789&mc=5192&mode=04&purpose=00")
        private String upiIntent;

        @Schema(description = "Generated payment link", example = "http://preprod.phon.pe/p3cgztr4")
        private String payLink;

        @Schema(description = "Customer's mobile number", example = "8296412345")
        private String mobileNumber;
    }
}


