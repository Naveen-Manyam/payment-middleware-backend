package com.phonepe.payment.staticqr.repository;

import com.phonepe.payment.staticqr.entity.StaticQRMetadataResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticQRMetadataResponseRepository extends JpaRepository<StaticQRMetadataResponse, Long> {
}