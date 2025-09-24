package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrCancelTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrCancelTransactionRequestRepository extends JpaRepository<DqrCancelTransactionRequest, Long> {
}

