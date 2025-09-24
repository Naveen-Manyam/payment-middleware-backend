package com.phonepe.payment.common.callback.repository;

import com.phonepe.payment.common.callback.entity.CallbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallbackRequestRepository extends JpaRepository<CallbackRequest, Long> {

    List<CallbackRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<CallbackRequest> findByErrorMessageIsNotNull();
}