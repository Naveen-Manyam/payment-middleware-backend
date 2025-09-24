package com.phonepe.payment.staticqr.repository;

import com.phonepe.payment.staticqr.entity.StaticQRCallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticQRCallbackRequestRepository extends JpaRepository<StaticQRCallbackRequest, Long> {
}