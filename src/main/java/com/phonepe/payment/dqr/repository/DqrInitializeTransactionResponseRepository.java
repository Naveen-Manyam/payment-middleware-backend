package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrInitializeTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrInitializeTransactionResponseRepository extends JpaRepository<DqrInitializeTransactionResponse, Long> {
}

