package com.phonepe.payment.collect.repository;

import com.phonepe.payment.collect.entity.CollectCallResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for CollectCallResponse entity operations.
 *
 * <p>This repository provides data access methods for PhonePe collect call response
 * entities, extending JpaRepository for standard CRUD operations and custom
 * query methods for collect call response management.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CollectCallResponse
 */
@Repository
public interface CollectCallResponseRepository extends JpaRepository<CollectCallResponse, Long> {
}