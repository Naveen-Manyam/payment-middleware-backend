package com.phonepe.payment.collect.repository;

import com.phonepe.payment.collect.entity.CollectCallRefundTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectCallRefundTransactionRequest
 * @see JpaRepository
 */
@Repository
public interface CollectCallRefundTransactionRequestRepository
        extends JpaRepository<CollectCallRefundTransactionRequest, Long> {
}