package com.phonepe.payment.paymentlink.repository;

import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link PaymentLinkCancelTransactionRequest} entity.
 *
 * <p>This repository provides data access operations for payment link transaction
 * cancellation requests. It extends {@link JpaRepository} to inherit standard
 * CRUD operations and query capabilities.
 *
 * <p>Available Operations:
 * <ul>
 *   <li>Standard CRUD operations (save, findById, findAll, delete)</li>
 *   <li>Custom query methods for business logic</li>
 *   <li>Pagination and sorting support</li>
 *   <li>Batch operations for performance optimization</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>{@code
 * @Autowired
 * private PaymentLinkCancelTransactionRequestRepository repository;
 *
 * public void saveCancelRequest(PaymentLinkCancelTransactionRequest request) {
 *     repository.save(request);
 * }
 * }</pre>
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkCancelTransactionRequest
 * @see JpaRepository
 */
@Repository
public interface PaymentLinkCancelTransactionRequestRepository
        extends JpaRepository<PaymentLinkCancelTransactionRequest, Long> {
}