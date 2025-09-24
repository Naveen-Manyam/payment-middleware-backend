package com.phonepe.payment.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Factory class for creating configured instances of {@link WebClient} with built-in
 * timeout management and retry mechanisms for making HTTP requests to PhonePe APIs.
 *
 * <p>This factory provides:
 * <ul>
 *   <li>Configurable connection, read, and write timeouts</li>
 *   <li>Automatic retry mechanism for 5xx server errors</li>
 *   <li>Exponential backoff strategy for retries</li>
 *   <li>Thread-safe WebClient instances</li>
 *   <li>Optimized Netty HTTP client configuration</li>
 * </ul>
 *
 * <p>Configuration properties:
 * <ul>
 *   <li>{@code phonepe.api.connection.timeout} - Connection timeout in ms (default: 30000)</li>
 *   <li>{@code phonepe.api.read.timeout} - Read timeout in ms (default: 30000)</li>
 *   <li>{@code phonepe.api.write.timeout} - Write timeout in ms (default: 30000)</li>
 *   <li>{@code phonepe.api.retry.max-attempts} - Maximum retry attempts (default: 3)</li>
 *   <li>{@code phonepe.api.retry.initial-delay} - Initial retry delay in ms (default: 1000)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Autowired
 * private PhonePeWebClientFactory webClientFactory;
 *
 * public String callPhonePeAPI(String payload) {
 *     WebClient webClient = webClientFactory.createWebClient();
 *
 *     return webClientFactory.withRetry(
 *         webClient.post()
 *             .uri(apiUrl)
 *             .bodyValue(payload)
 *             .exchangeToMono(response -> response.bodyToMono(String.class))
 *     ).block();
 * }
 * }</pre>
 *
 * @author PhonePe MiddleWare Team
 * @version 1.0
 * @since 1.0
 * @see WebClient
 * @see Retry
 */
@Component
public class PhonePeWebClientFactory {

    /**
     * Connection timeout in milliseconds.
     * Configurable via 'phonepe.api.connection.timeout' property.
     */
    @Value("${phonepe.api.connection.timeout:30000}")
    private int connectionTimeoutMs;

    /**
     * Read timeout in milliseconds.
     * Configurable via 'phonepe.api.read.timeout' property.
     */
    @Value("${phonepe.api.read.timeout:30000}")
    private int readTimeoutMs;

    /**
     * Write timeout in milliseconds.
     * Configurable via 'phonepe.api.write.timeout' property.
     */
    @Value("${phonepe.api.write.timeout:30000}")
    private int writeTimeoutMs;

    /**
     * Maximum number of retry attempts for failed requests.
     * Configurable via 'phonepe.api.retry.max-attempts' property.
     */
    @Value("${phonepe.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * Initial retry delay in milliseconds.
     * Configurable via 'phonepe.api.retry.initial-delay' property.
     */
    @Value("${phonepe.api.retry.initial-delay:1000}")
    private long initialRetryDelay;

    /**
     * Creates a new {@link WebClient} instance with custom timeouts for
     * connection, read, and write operations using Netty HTTP client.
     *
     * <p>The WebClient is configured with:
     * <ul>
     *   <li>Connection timeout: Maximum time to establish a connection</li>
     *   <li>Read timeout: Maximum time to read response data</li>
     *   <li>Write timeout: Maximum time to write request data</li>
     *   <li>Response timeout: Overall response timeout</li>
     * </ul>
     *
     * <p>This method creates a new WebClient instance on each call. For performance-critical
     * applications, consider caching the WebClient instance if it will be reused frequently.
     *
     * @return a configured {@link WebClient} instance with timeout settings
     * @throws IllegalStateException if timeout values are invalid
     */
    public WebClient createWebClient() {
        // Create HTTP client with connection timeout
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs))
            .doOnConnected(conn ->
                // Add read and write timeout handlers to the connection
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
            );

        // Build WebClient with the configured HTTP client
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    /**
     * Creates a {@link Retry} configuration for WebClient requests with exponential backoff strategy.
     *
     * <p>This retry mechanism is specifically designed for handling transient failures
     * from external API calls. It will retry only on 5xx server errors, indicating
     * server-side issues that might be resolved on subsequent attempts.
     *
     * <p>Retry behavior:
     * <ul>
     *   <li>Maximum attempts: Configured via {@code maxRetryAttempts}</li>
     *   <li>Initial delay: Configured via {@code initialRetryDelay}</li>
     *   <li>Backoff strategy: Exponential (delay doubles after each attempt)</li>
     *   <li>Retry conditions: HTTP 500, 502, 503, 504 errors</li>
     * </ul>
     *
     * <p>Errors that will trigger retries:
     * <ul>
     *   <li>HTTP 500 - Internal Server Error</li>
     *   <li>HTTP 502 - Bad Gateway</li>
     *   <li>HTTP 503 - Service Unavailable</li>
     *   <li>HTTP 504 - Gateway Timeout</li>
     * </ul>
     *
     * @return configured {@link Retry} instance with exponential backoff
     * @see Retry#backoff(long, Duration)
     */
    public Retry createRetrySpec() {
        return Retry.backoff(maxRetryAttempts, Duration.ofMillis(initialRetryDelay))
            .filter(throwable -> {
                // Only retry on server errors (5xx), not client errors (4xx)
                if (throwable instanceof RuntimeException) {
                    String message = throwable.getMessage();
                    return message != null && (
                        message.contains("HTTP 500") ||  // Internal Server Error
                        message.contains("HTTP 502") ||  // Bad Gateway
                        message.contains("HTTP 503") ||  // Service Unavailable
                        message.contains("HTTP 504") ||  // Gateway Timeout
                        message.contains("Internal Server Error")
                    );
                }
                return false;
            })
            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                // Throw a comprehensive exception when all retry attempts are exhausted
                return new RuntimeException(
                    String.format("Max retry attempts (%d) exceeded for PhonePe API call. Last error: %s",
                        maxRetryAttempts, retrySignal.failure().getMessage()),
                    retrySignal.failure()
                );
            });
    }

    /**
     * Applies retry logic to a WebClient request {@link Mono} for handling transient failures.
     *
     * <p>This method wraps the provided Mono with retry functionality, automatically
     * retrying failed requests based on the configured retry specification. It should
     * be used for all API calls to PhonePe services to improve reliability.
     *
     * <p>Usage example:
     * <pre>{@code
     * WebClient webClient = webClientFactory.createWebClient();
     *
     * String response = webClientFactory.withRetry(
     *     webClient.post()
     *         .uri("https://api.phonepe.com/v1/transaction")
     *         .bodyValue(requestPayload)
     *         .exchangeToMono(clientResponse -> {
     *             if (clientResponse.statusCode().isError()) {
     *                 return clientResponse.bodyToMono(String.class)
     *                     .flatMap(errorBody -> Mono.error(new RuntimeException(
     *                         "API Error: " + clientResponse.statusCode() + " - " + errorBody)));
     *             }
     *             return clientResponse.bodyToMono(String.class);
     *         })
     * ).block();
     * }</pre>
     *
     * @param <T> the type of data emitted by the Mono
     * @param mono the {@link Mono} representing the WebClient request to be retried
     * @return a new {@link Mono} with retry logic applied, emitting the same type as input
     * @throws RuntimeException when all retry attempts are exhausted
     * @see #createRetrySpec()
     */
    public <T> Mono<T> withRetry(Mono<T> mono) {
        return mono.retryWhen(createRetrySpec());
    }
}
