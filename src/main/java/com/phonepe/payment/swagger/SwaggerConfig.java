package com.phonepe.payment.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for PhonePe MiddleWare API documentation.
 *
 * <p>This configuration class sets up OpenAPI documentation for the PhonePe payment
 * gateway middleware, providing interactive API documentation for all payment
 * endpoints including DQR, Static QR, EDC, and Payment Link operations.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PhonePe Middleware API")
                        .version("1.0")
                        .description("Spring Boot integration with PhonePe APIs (QR Init, Cancel, Refund, Check Status)."));
    }
}
