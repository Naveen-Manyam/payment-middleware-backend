package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkCheckTransactionStatusResponse} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * status check responses received from PhonePe API. It stores the status information
 * for tracking transaction lifecycle and customer service support.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCheckTransactionStatusResponse
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkCheckTransactionStatusResponseRepository
        extends JpaRepository<PaymentLinkCheckTransactionStatusResponse, Long> {
}