package com.phonepe.payment.edc.repository;

import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdcCheckTransactionStatusRequestRepository extends JpaRepository<EdcCheckTransactionStatusRequest, Long> {
}

