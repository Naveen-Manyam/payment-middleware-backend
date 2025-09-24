package com.phonepe.payment.staticqr.repository;

import com.phonepe.payment.staticqr.entity.StaticQRTransactionListRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticQRTransactionListRequestRepository extends JpaRepository<StaticQRTransactionListRequest, Long> {
}

