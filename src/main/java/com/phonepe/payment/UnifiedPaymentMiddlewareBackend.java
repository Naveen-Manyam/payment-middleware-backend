package com.phonepe.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for PhonePe MiddleWare.
 *
 * <p>This is the entry point for the PhonePe payment gateway middleware application
 * that provides unified APIs for multiple payment methods including Dynamic QR (DQR),
 * Static QR, Electronic Data Capture (EDC), and Payment Links.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
public class UnifiedPaymentMiddlewareBackend {
	public static void main(String[] args) {
		SpringApplication.run(UnifiedPaymentMiddlewareBackend.class, args);
	}
}
