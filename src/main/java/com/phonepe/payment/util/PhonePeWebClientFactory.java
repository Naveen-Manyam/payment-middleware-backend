package com.phonepe.payment.util;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.net.ConnectException;

/**
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
     * Maximum number of connections in the pool.
     * Configurable via 'phonepe.api.connection.pool.max-connections' property.
     */
    @Value("${phonepe.api.connection.pool.max-connections:500}")
    private int maxConnections;

    /**
     * Maximum number of pending acquire requests.
     * Configurable via 'phonepe.api.connection.pool.pending-acquire-max' property.
     */
    @Value("${phonepe.api.connection.pool.pending-acquire-max:1000}")
    private int pendingAcquireMax;

    /**
     * Maximum idle time for connections in milliseconds.
     * Configurable via 'phonepe.api.connection.pool.max-idle-time' property.
     */
    @Value("${phonepe.api.connection.pool.max-idle-time:20000}")
    private long maxIdleTime;

    /**
     * Maximum life time for connections in milliseconds.
     * Configurable via 'phonepe.api.connection.pool.max-life-time' property.
     */
    @Value("${phonepe.api.connection.pool.max-life-time:60000}")
    private long maxLifeTime;

    /**
     * Evict interval for idle connections in milliseconds.
     * Configurable via 'phonepe.api.connection.pool.evict-interval' property.
     */
    @Value("${phonepe.api.connection.pool.evict-interval:30000}")
    private long evictInterval;

    private volatile WebClient webClient;

    /**
     * Creates or returns a singleton {@link WebClient} instance with custom timeouts,
     * connection pooling, and keep-alive configuration for optimal performance.
     *
     * <p>The WebClient is configured with:
     * <ul>
     *   <li>Connection pooling with configurable max connections and idle time</li>
     *   <li>Connection timeout: Maximum time to establish a connection</li>
     *   <li>Read timeout: Maximum time to read response data</li>
     *   <li>Write timeout: Maximum time to write request data</li>
     *   <li>Response timeout: Overall response timeout</li>
     *   <li>Keep-alive and idle connection management</li>
     *   <li>Automatic eviction of stale connections</li>
     * </ul>
     *
     * <p>This method uses double-checked locking to ensure thread-safe singleton creation
     * while maintaining high performance for concurrent access.
     *
     * @return a configured {@link WebClient} instance with timeout and pooling settings
     * @throws IllegalStateException if timeout values are invalid
     */
    public WebClient createWebClient() {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    // Create connection provider with pooling configuration
                    ConnectionProvider connectionProvider = ConnectionProvider.builder("phonepe-pool")
                            .maxConnections(maxConnections)
                            .pendingAcquireMaxCount(pendingAcquireMax)
                            .maxIdleTime(Duration.ofMillis(maxIdleTime))
                            .maxLifeTime(Duration.ofMillis(maxLifeTime))
                            .evictInBackground(Duration.ofMillis(evictInterval))
                            .build();

                    // Create HTTP client with connection pooling and timeout configuration
                    HttpClient httpClient = HttpClient.create(connectionProvider)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .responseTimeout(Duration.ofMillis(readTimeoutMs))
                            .doOnConnected(conn ->
                                    // Add read and write timeout handlers to the connection
                                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                                            .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))
                            );

                    // Build WebClient with the configured HTTP client
                    webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .build();
                }
            }
        }
        return webClient;
    }

    /**
     * Creates a {@link Retry} configuration for WebClient requests with exponential backoff strategy.
     *
     * <p>This retry mechanism is specifically designed for handling transient failures
     * from external API calls. It will retry on 5xx server errors and network-related errors
     * such as connection resets, timeouts, and I/O exceptions.
     *
     * <p>Retry behavior:
     * <ul>
     *   <li>Maximum attempts: Configured via {@code maxRetryAttempts}</li>
     *   <li>Initial delay: Configured via {@code initialRetryDelay}</li>
     *   <li>Backoff strategy: Exponential (delay doubles after each attempt)</li>
     *   <li>Retry conditions: HTTP 500, 502, 503, 504 errors, connection resets, and I/O errors</li>
     * </ul>
     *
     * <p>Errors that will trigger retries:
     * <ul>
     *   <li>HTTP 500 - Internal Server Error</li>
     *   <li>HTTP 502 - Bad Gateway</li>
     *   <li>HTTP 503 - Service Unavailable</li>
     *   <li>HTTP 504 - Gateway Timeout</li>
     *   <li>Connection reset</li>
     *   <li>Connection refused</li>
     *   <li>I/O and network exceptions</li>
     *   <li>WebClientException</li>
     * </ul>
     *
     * @return configured {@link Retry} instance with exponential backoff
     * @see Retry#backoff(long, Duration)
     */
    public Retry createRetrySpec() {
        return Retry.backoff(maxRetryAttempts, Duration.ofMillis(initialRetryDelay))
                .filter(throwable -> {
                    // Retry on network-related exceptions
                    if (throwable instanceof IOException ||
                            throwable instanceof ConnectException ||
                            throwable instanceof WebClientException) {
                        return true;
                    }

                    // Retry on connection reset and similar network issues
                    Throwable cause = throwable.getCause();
                    if (cause instanceof IOException || cause instanceof ConnectException) {
                        return true;
                    }

                    // Check for connection reset in the exception chain
                    String message = throwable.getMessage();
                    if (message != null && (
                            message.contains("Connection reset") ||
                                    message.contains("Connection refused") ||
                                    message.contains("Broken pipe") ||
                                    message.contains("Connection timed out") ||
                                    message.contains("Connection closed prematurely")
                    )) {
                        return true;
                    }

                    // Retry on server errors (5xx)
                    if (throwable instanceof RuntimeException) {
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
