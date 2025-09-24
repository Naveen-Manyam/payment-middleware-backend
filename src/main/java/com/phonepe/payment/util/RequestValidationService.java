package com.phonepe.payment.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Global request validation service for validating required fields across all controllers.
 *
 * <p>This service provides centralized validation functionality to ensure that all
 * required fields are present in incoming requests. It automatically checks for null
 * or empty required fields and returns standardized FailureApiResponse objects.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Automatic validation of required fields using reflection</li>
 *   <li>Standardized error responses with FailureApiResponse</li>
 *   <li>Support for custom validation rules</li>
 *   <li>Null and empty string validation</li>
 *   <li>Business rule validation for specific field types</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@Component
public class RequestValidationService {

    /**
     * Validates required fields in a request object.
     *
     * @param request The request object to validate
     * @param requiredFields Array of field names that must be present and non-null
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateRequiredFields(Object request, String... requiredFields) {
        if (request == null) {
            return createValidationFailureResponse("Request body is required");
        }

        List<String> missingFields = new ArrayList<>();

        for (String fieldName : requiredFields) {
            try {
                Field field = getField(request.getClass(), fieldName);
                if (field == null) {
                    missingFields.add(fieldName);
                    continue;
                }

                field.setAccessible(true);
                Object value = field.get(request);

                if (isFieldEmpty(value)) {
                    missingFields.add(fieldName);
                }
            } catch (IllegalAccessException e) {
                missingFields.add(fieldName);
            }
        }

        if (!missingFields.isEmpty()) {
            String message = buildMissingFieldsMessage(missingFields);
            return createValidationFailureResponse(message);
        }

        return null; // Validation passed
    }

    /**
     * Validates business rules for common field types.
     *
     * @param request The request object to validate
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateBusinessRules(Object request) {
        if (request == null) {
            return createValidationFailureResponse("Request body is required");
        }

        try {
            // Validate amount field if present
            String amountValidationError = validateAmount(request);
            if (amountValidationError != null) {
                return createValidationFailureResponse(amountValidationError);
            }

            // Validate transaction ID if present
            String transactionIdValidationError = validateTransactionId(request);
            if (transactionIdValidationError != null) {
                return createValidationFailureResponse(transactionIdValidationError);
            }

            // Validate merchant ID if present
            String merchantIdValidationError = validateMerchantId(request);
            if (merchantIdValidationError != null) {
                return createValidationFailureResponse(merchantIdValidationError);
            }

            // Validate mobile number if present
            String mobileValidationError = validateMobileNumber(request);
            if (mobileValidationError != null) {
                return createValidationFailureResponse(mobileValidationError);
            }

            // Validate email if present
            String emailValidationError = validateEmail(request);
            if (emailValidationError != null) {
                return createValidationFailureResponse(emailValidationError);
            }

        } catch (Exception e) {
            return createValidationFailureResponse("Error validating request fields: " + e.getMessage());
        }

        return null; // Validation passed
    }

    /**
     * Comprehensive validation method that checks both required fields and business rules.
     *
     * @param request The request object to validate
     * @param requiredFields Array of field names that must be present and non-null
     * @return ResponseEntity with FailureApiResponse if validation fails, null if validation passes
     */
    public ResponseEntity<FailureApiResponse<Object>> validateRequest(Object request, String... requiredFields) {
        // First validate required fields
        ResponseEntity<FailureApiResponse<Object>> requiredFieldsResult = validateRequiredFields(request, requiredFields);
        if (requiredFieldsResult != null) {
            return requiredFieldsResult;
        }

        // Then validate business rules
        return validateBusinessRules(request);
    }

    /**
     * Validates amount field for positive values.
     */
    private String validateAmount(Object request) throws IllegalAccessException {
        Field amountField = getField(request.getClass(), "amount");
        if (amountField != null) {
            amountField.setAccessible(true);
            Object amountValue = amountField.get(request);

            if (amountValue != null) {
                Number amount = null;
                if (amountValue instanceof Number) {
                    amount = (Number) amountValue;
                } else if (amountValue instanceof String) {
                    try {
                        amount = Double.parseDouble((String) amountValue);
                    } catch (NumberFormatException e) {
                        return "Amount must be a valid number";
                    }
                }

                if (amount != null && amount.doubleValue() <= 0) {
                    return "Amount must be greater than zero";
                }
            }
        }
        return null;
    }

    /**
     * Validates transaction ID format.
     */
    private String validateTransactionId(Object request) throws IllegalAccessException {
        Field transactionIdField = getField(request.getClass(), "transactionId");
        if (transactionIdField != null) {
            transactionIdField.setAccessible(true);
            Object transactionIdValue = transactionIdField.get(request);

            if (transactionIdValue != null && transactionIdValue instanceof String) {
                String transactionId = (String) transactionIdValue;
                if (transactionId.trim().length() < 3) {
                    return "Transaction ID must be at least 3 characters long";
                }
                if (transactionId.trim().length() > 50) {
                    return "Transaction ID must not exceed 50 characters";
                }
            }
        }
        return null;
    }

    /**
     * Validates merchant ID format.
     */
    private String validateMerchantId(Object request) throws IllegalAccessException {
        Field merchantIdField = getField(request.getClass(), "merchantId");
        if (merchantIdField != null) {
            merchantIdField.setAccessible(true);
            Object merchantIdValue = merchantIdField.get(request);

            if (merchantIdValue != null && merchantIdValue instanceof String) {
                String merchantId = (String) merchantIdValue;
                if (merchantId.trim().length() < 3) {
                    return "Merchant ID must be at least 3 characters long";
                }
                if (merchantId.trim().length() > 50) {
                    return "Merchant ID must not exceed 50 characters";
                }
            }
        }
        return null;
    }

    /**
     * Validates mobile number format.
     */
    private String validateMobileNumber(Object request) throws IllegalAccessException {
        String[] possibleFields = {"instrumentReference", "mobileNumber", "mobile"};

        for (String fieldName : possibleFields) {
            Field mobileField = getField(request.getClass(), fieldName);
            if (mobileField != null) {
                mobileField.setAccessible(true);
                Object mobileValue = mobileField.get(request);

                if (mobileValue != null && mobileValue instanceof String) {
                    String mobile = (String) mobileValue;
                    // Basic mobile number validation (Indian format)
                    if (!mobile.matches("^[6-9]\\d{9}$")) {
                        return fieldName + " must be a valid 10-digit Indian mobile number";
                    }
                }
            }
        }
        return null;
    }

    /**
     * Validates email format.
     */
    private String validateEmail(Object request) throws IllegalAccessException {
        Field emailField = getField(request.getClass(), "email");
        if (emailField != null) {
            emailField.setAccessible(true);
            Object emailValue = emailField.get(request);

            if (emailValue != null && emailValue instanceof String) {
                String email = (String) emailValue;
                // Basic email validation
                if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    return "Email must be a valid email address";
                }
            }
        }
        return null;
    }

    /**
     * Helper method to get field from class hierarchy.
     */
    private Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Checks if a field value is considered empty.
     */
    private boolean isFieldEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return false;
    }

    /**
     * Builds a user-friendly message for missing fields.
     */
    private String buildMissingFieldsMessage(List<String> missingFields) {
        if (missingFields.size() == 1) {
            return missingFields.get(0) + " is required";
        } else {
            return "Required fields are missing: " + String.join(", ", missingFields);
        }
    }

    /**
     * Creates a standardized validation failure response.
     */
    private ResponseEntity<FailureApiResponse<Object>> createValidationFailureResponse(String message) {
        FailureApiResponse<Object> failureResponse = FailureApiResponse.<Object>builder()
                .success(false)
                .code("VALIDATION_ERROR")
                .message(message)
                .data(null)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failureResponse);
    }
}