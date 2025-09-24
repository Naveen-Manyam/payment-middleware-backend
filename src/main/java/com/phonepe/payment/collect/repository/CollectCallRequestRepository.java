package com.phonepe.payment.collect.repository;

import com.phonepe.payment.collect.entity.CollectCallRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for CollectCallRequest entity operations.
 *
 * <p>This repository provides data access methods for PhonePe collect call request
 * entities, extending JpaRepository for standard CRUD operations and custom
 * query methods for collect call request management.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectCallRequest
 */
@Repository
public interface CollectCallRequestRepository extends JpaRepository<CollectCallRequest, Long> {
}