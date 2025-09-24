package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkCheckTransactionStatusRequest} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * status check requests. It stores records of all status inquiries for audit
 * and tracking purposes.
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCheckTransactionStatusRequest
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkCheckTransactionStatusRequestRepository
        extends JpaRepository<PaymentLinkCheckTransactionStatusRequest, Long> {
}