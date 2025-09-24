package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrCheckTransactionStatusResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrCheckTransactionStatusResponseRepository extends JpaRepository<DqrCheckTransactionStatusResponse, Long> {
}

