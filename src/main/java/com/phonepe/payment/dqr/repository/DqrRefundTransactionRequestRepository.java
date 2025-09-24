package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrRefundTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrRefundTransactionRequestRepository extends JpaRepository<DqrRefundTransactionRequest, Long> {
}

