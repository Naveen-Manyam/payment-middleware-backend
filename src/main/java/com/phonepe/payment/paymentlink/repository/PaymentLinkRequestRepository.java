package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.edc.entity.EdcInitializeTransactionRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLinkRequestRepository extends JpaRepository<PaymentLinkRequest, Long> {
}

