package com.phonepe.payment.edc.repository;

import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdcCheckTransactionStatusResponseRepository extends JpaRepository<EdcCheckTransactionStatusResponse, Long> {
}

