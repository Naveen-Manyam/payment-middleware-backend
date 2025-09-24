package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkRefundTransactionRequest} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * refund requests. It stores records of all refund operations for audit,
 * reconciliation, and financial reporting purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkRefundTransactionRequest
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkRefundTransactionRequestRepository
        extends JpaRepository<PaymentLinkRefundTransactionRequest, Long> {
}