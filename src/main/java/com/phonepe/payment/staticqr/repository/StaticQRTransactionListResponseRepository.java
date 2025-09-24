package com.phonepe.payment.staticqr.repository;

import com.phonepe.payment.staticqr.entity.StaticQRTransactionListResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaticQRTransactionListResponseRepository extends JpaRepository<StaticQRTransactionListResponse, Long> {
}

