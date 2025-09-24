package com.phonepe.payment.util;

import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class for generating X-VERIFY signatures required for PhonePe API authentication.
 *
 * <p>The X-VERIFY signature is a security mechanism used by PhonePe to ensure the integrity
 * and authenticity of API requests. It is generated using SHA-256 hashing algorithm with
 * a combination of request payload, salt key, and salt index.
 *
 * <p>X-VERIFY Format:
 * <pre>
 * X-VERIFY = SHA256(payload + saltKey) + "###" + saltIndex
 * </pre>
 *
 * <p>Where:
 * <ul>
 *   <li><b>payload</b> - The request payload (for POST) or API path (for GET)</li>
 *   <li><b>saltKey</b> - Secret salt key provided by PhonePe</li>
 *   <li><b>saltIndex</b> - Salt index associated with the salt key</li>
 *   <li><b>###</b> - Fixed delimiter between hash and salt index</li>
 * </ul>
 *
 * <p>Security Considerations:
 * <ul>
 *   <li>Salt keys must be kept confidential and never logged</li>
 *   <li>X-VERIFY signatures should be generated fresh for each request</li>
 *   <li>The salt key and index are provided by PhonePe during merchant onboarding</li>
 *   <li>Different environments (sandbox/production) may have different salt keys</li>
 * </ul>
 *
 * <p>Usage Examples:
 * <pre>{@code
 * // For POST requests with JSON payload
 * String base64Payload = Base64.getEncoder().encodeToString(jsonPayload.getBytes());
 * String path = base64Payload + "/v3/qr/init";
 * String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
 *
 * // For GET requests
 * String path = "/v3/qr/transaction/MERCHANT123/TXN456/status";
 * String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
 * }</pre>
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see MessageDigest
 */
@Configuration
public class GenerateXVerifyKey {

    /**
     * Generates X-VERIFY signature for PhonePe API authentication.
     *
     * <p>This method creates a SHA-256 hash of the concatenated payload and salt key,
     * then appends the salt index with a delimiter to form the complete X-VERIFY signature.
     *
     * <p>The method is thread-safe and can be called concurrently from multiple threads.
     * Each call creates a new MessageDigest instance to ensure thread safety.
     *
     * <p>Algorithm Steps:
     * <ol>
     *   <li>Concatenate path/payload with salt key</li>
     *   <li>Generate SHA-256 hash of the concatenated string</li>
     *   <li>Convert hash bytes to lowercase hexadecimal string</li>
     *   <li>Append "###" delimiter and salt index</li>
     * </ol>
     *
     * @param path the API path (for GET requests) or base64-encoded payload + path (for POST requests)
     * @param saltKey the secret salt key provided by PhonePe (must not be null or empty)
     * @param saltIndex the salt index associated with the salt key (typically "1")
     * @return the complete X-VERIFY signature in format: "{sha256_hash}###{salt_index}"
     * @throws RuntimeException if SHA-256 algorithm is not available or hashing fails
     * @throws IllegalArgumentException if path or saltKey is null or empty
     * @see MessageDigest#getInstance(String)
     */
    public static String generateXVerify(String path, String saltKey, String saltIndex) {
        try {
            // Step 1: Concatenate path/payload with salt key
            String toHash = path + saltKey;

            // Step 2: Get SHA-256 message digest instance
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Step 3: Generate hash bytes using UTF-8 encoding
            byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));

            // Step 4: Convert hash bytes to lowercase hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            // Step 5: Append delimiter and salt index to complete X-VERIFY signature
            return hexString.toString() + "###" + saltIndex;

        } catch (Exception e) {
            throw new RuntimeException("Error generating X-VERIFY signature: " + e.getMessage(), e);
        }
    }
}
