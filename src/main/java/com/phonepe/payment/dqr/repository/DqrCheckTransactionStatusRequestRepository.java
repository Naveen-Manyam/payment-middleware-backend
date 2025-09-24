package com.phonepe.payment.dqr.repository;

import com.phonepe.payment.dqr.entity.DqrCheckTransactionStatusRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqrCheckTransactionStatusRequestRepository extends JpaRepository<DqrCheckTransactionStatusRequest, Long> {
}

