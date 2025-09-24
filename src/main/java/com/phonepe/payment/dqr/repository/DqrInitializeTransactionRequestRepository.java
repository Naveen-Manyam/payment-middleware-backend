package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrInitializeTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrInitializeTransactionRequestRepository extends JpaRepository<DqrInitializeTransactionRequest, Long> {
}

