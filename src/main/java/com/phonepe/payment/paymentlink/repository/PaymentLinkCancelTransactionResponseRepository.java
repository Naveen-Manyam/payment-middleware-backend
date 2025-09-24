package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkCancelTransactionResponse} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * cancellation responses received from PhonePe API. It stores the results of
 * cancellation operations for audit and troubleshooting purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCancelTransactionResponse
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkCancelTransactionResponseRepository
        extends JpaRepository<PaymentLinkCancelTransactionResponse, Long> {
}