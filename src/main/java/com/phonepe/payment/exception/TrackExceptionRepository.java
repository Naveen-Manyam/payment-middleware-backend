package com.phonepe.payment.exception;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackExceptionRepository extends JpaRepository<ExceptionTrackResponse, Long> {
}
