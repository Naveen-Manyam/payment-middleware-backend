package com.phonepe.payment.util;

import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

/**
 * Utility class for generating unique transaction identifiers.
 *
 * <p>This class provides functionality to generate secure, unique transaction IDs
 * that follow a specific format for the PhonePe payment gateway integration.
 * The generated IDs are cryptographically secure using SecureRandom.</p>
 *
 * <p>Transaction ID Format:
 * <ul>
 *   <li>Prefix: "TX" (2 characters)</li>
 *   <li>Random digits: 14 characters from provided numeric string</li>
 *   <li>Total length: 16 characters</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see SecureRandom
 */
@Configuration
public class GenerateTransactionId {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static String generateTransactionId(String numeric){
        StringBuilder sb = new StringBuilder("TX"); // start with TX
        for (int i = 0; i < 14; i++) { // remaining 8 chars
            int index = RANDOM.nextInt(numeric.length());
            sb.append(numeric.charAt(index));
        }
        return sb.toString();
    }
}
