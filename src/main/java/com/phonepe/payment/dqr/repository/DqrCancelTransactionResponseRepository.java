package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrCancelTransactionResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrCancelTransactionResponseRepository extends JpaRepository<DqrCancelTransactionResponse, Long> {
}

