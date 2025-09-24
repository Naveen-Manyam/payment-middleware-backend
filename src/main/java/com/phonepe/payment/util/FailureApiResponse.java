package com.phonepe.payment.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic failure response wrapper for API operations.
 *
 * <p>This class provides a standardized structure for representing failed API
 * responses across the PhonePe middleware application. It ensures consistent
 * error reporting format for all service operations and supports generic data
 * payloads for additional error context.</p>
 *
 * <p>Response Structure:
 * <ul>
 *   <li><b>success</b> - Always false for failure responses</li>
 *   <li><b>code</b> - Error code for programmatic error handling</li>
 *   <li><b>message</b> - Human-readable error message</li>
 *   <li><b>data</b> - Optional additional error context</li>
 * </ul>
 *
 * @param <T> the type of additional data to include in the response
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}
