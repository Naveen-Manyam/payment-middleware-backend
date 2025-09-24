package com.phonepe.payment.edc.repository;

import com.phonepe.payment.edc.entity.EdcInitializeTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdcInitializeTransactionRequestRepository extends JpaRepository<EdcInitializeTransactionRequest, Long> {
}

