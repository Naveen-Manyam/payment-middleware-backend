package com.phonepe.payment.collect.repository;

import com.phonepe.payment.collect.entity.CollectCallRefundTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see CollectCallRefundTransactionResponse
 * @see JpaRepository
 */
@Repository
public interface CollectCallRefundTransactionResponseRepository
        extends JpaRepository<CollectCallRefundTransactionResponse, Long> {
}