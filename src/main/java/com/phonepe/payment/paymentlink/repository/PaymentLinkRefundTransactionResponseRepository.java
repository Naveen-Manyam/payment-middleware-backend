package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkRefundTransactionResponse} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * refund responses received from PhonePe API. It stores the results of refund
 * operations for tracking and customer service purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkRefundTransactionResponse
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkRefundTransactionResponseRepository
        extends JpaRepository<PaymentLinkRefundTransactionResponse, Long> {
}