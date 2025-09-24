package com.phonepe.payment.collect.repository;

import com.phonepe.payment.collect.entity.CollectCallCancelTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see CollectCallCancelTransactionRequest
 * @see JpaRepository
 */
@Repository
public interface CollectCallCancelTransactionRequestRepository
        extends JpaRepository<CollectCallCancelTransactionRequest, Long> {
}