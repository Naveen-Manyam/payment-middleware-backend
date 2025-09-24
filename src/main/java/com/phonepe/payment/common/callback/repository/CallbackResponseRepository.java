package com.phonepe.payment.common.callback.repository;

import com.phonepe.payment.common.callback.entity.CallbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallbackResponseRepository extends JpaRepository<CallbackResponse, Long> {

    List<CallbackResponse> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<CallbackResponse> findByTransactionId(String transactionId);

    List<CallbackResponse> findByMerchantId(String merchantId);

    List<CallbackResponse> findByPaymentState(String paymentState);

    List<CallbackResponse> findByPayResponseCode(String payResponseCode);
}