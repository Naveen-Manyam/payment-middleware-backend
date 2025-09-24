package com.phonepe.payment.staticqr.repository;

import com.phonepe.payment.staticqr.entity.StaticQRMetadataRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticQRMetadataRequestRepository extends JpaRepository<StaticQRMetadataRequest, Long> {
}