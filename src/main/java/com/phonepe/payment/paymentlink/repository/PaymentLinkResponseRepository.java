package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.edc.entity.EdcInitializeTransactionResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkResponseRepository extends JpaRepository<PaymentLinkResponse, Long> {
}

