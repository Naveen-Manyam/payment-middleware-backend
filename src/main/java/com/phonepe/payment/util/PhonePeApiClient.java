package com.phonepe.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP client utility for making secure API calls to PhonePe payment gateway.
 *
 * <p>This client provides a high-level interface for communicating with PhonePe APIs,
 * handling authentication, request/response formatting, and logging. It abstracts
 * the complexity of X-VERIFY signature generation, Base64 encoding, and proper
 * header management required by PhonePe's API specification.</p>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Automatic X-VERIFY signature generation for authentication</li>
 *   <li>Base64 encoding/decoding of request payloads</li>
 *   <li>Structured logging for API calls and responses</li>
 *   <li>Support for both POST and GET requests</li>
 *   <li>Custom header management</li>
 *   <li>Integration with PhonePeWebClientFactory for timeout and retry handling</li>
 * </ul>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see PhonePeWebClientFactory
 * @see GenerateXVerifyKey
 */
@Component
@Slf4j
public class PhonePeApiClient {

    @Autowired
    private PhonePeWebClientFactory webClientFactory;

    @Autowired
    private ObjectMapper objectMapper;

    public static class ApiRequest {
        private final String baseUrl;
        private final String endpoint;
        private final Object requestPayload;
        private final String saltKey;
        private final String saltIndex;
        private final Map<String, String> headers;

        public ApiRequest(String baseUrl, String endpoint, Object requestPayload,
                         String saltKey, String saltIndex) {
            this.baseUrl = baseUrl;
            this.endpoint = endpoint;
            this.requestPayload = requestPayload;
            this.saltKey = saltKey;
            this.saltIndex = saltIndex;
            this.headers = new HashMap<>();
        }

        public ApiRequest addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public String getBaseUrl() { return baseUrl; }
        public String getEndpoint() { return endpoint; }
        public Object getRequestPayload() { return requestPayload; }
        public String getSaltKey() { return saltKey; }
        public String getSaltIndex() { return saltIndex; }
        public Map<String, String> getHeaders() { return headers; }
    }

    public String makePostRequest(ApiRequest apiRequest, String transactionId) throws JsonProcessingException {
        // Convert payload to JSON and encode
        String jsonBody = objectMapper.writeValueAsString(apiRequest.getRequestPayload());
        JSONObject params = new JSONObject(jsonBody);
        String jsonString = params.toString();
        byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
        String base64EncodedString = new String(encodedBytes);

        // Generate X-VERIFY
        String path = base64EncodedString + apiRequest.getEndpoint();
        String xVerify = GenerateXVerifyKey.generateXVerify(path, apiRequest.getSaltKey(), apiRequest.getSaltIndex());

        // Create request wrapper
        Map<String, String> wrapper = new HashMap<>();
        wrapper.put("request", base64EncodedString);

        // Log API call
        StructuredLogger.Patterns.logPhonepeApiCall(log, "PHONEPE_API_CALL",
            apiRequest.getBaseUrl() + apiRequest.getEndpoint(), wrapper, transactionId);

        // Create WebClient and make request
        WebClient webClient = webClientFactory.createWebClient();
        long callStart = System.currentTimeMillis();

        WebClient.RequestBodySpec requestSpec = webClient.post()
            .uri(apiRequest.getBaseUrl() + apiRequest.getEndpoint())
            .header("X-VERIFY", xVerify)
            .header("Content-Type", "application/json")
            .contentType(MediaType.APPLICATION_JSON);

        // Add custom headers
        for (Map.Entry<String, String> header : apiRequest.getHeaders().entrySet()) {
            requestSpec = requestSpec.header(header.getKey(), header.getValue());
        }

        String response = requestSpec
            .bodyValue(wrapper)
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
            .block();

        // Log API response
        StructuredLogger.Patterns.logPhonepeApiResponse(log, "PHONEPE_API_CALL",
            apiRequest.getBaseUrl() + apiRequest.getEndpoint(), response,
            System.currentTimeMillis() - callStart, 200);

        return response;
    }

    public String makeGetRequest(String baseUrl, String fullPath, String saltKey,
                                String saltIndex, Map<String, String> headers, String transactionId) {
        // Generate X-VERIFY for GET request
        String xVerify = GenerateXVerifyKey.generateXVerify(fullPath, saltKey, saltIndex);

        // Create WebClient and make request
        WebClient webClient = webClientFactory.createWebClient();
        long callStart = System.currentTimeMillis();

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
            .uri(baseUrl + fullPath)
            .header("X-VERIFY", xVerify)
            .header("Content-Type", "application/json");

        // Add custom headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestSpec = requestSpec.header(header.getKey(), header.getValue());
        }

        String response = requestSpec
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
            .block();

        // Log API response
        StructuredLogger.Patterns.logPhonepeApiResponse(log, "PHONEPE_API_GET_CALL",
            baseUrl + fullPath, response, System.currentTimeMillis() - callStart, 200);

        return response;
    }
}