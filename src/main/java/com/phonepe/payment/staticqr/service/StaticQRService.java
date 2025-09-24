package com.phonepe.payment.staticqr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.staticqr.constants.StaticQRResponseCode;
import com.phonepe.payment.staticqr.entity.*;
import com.phonepe.payment.staticqr.exception.StaticQRApiException;
import com.phonepe.payment.staticqr.repository.StaticQRTransactionListRequestRepository;
import com.phonepe.payment.staticqr.repository.StaticQRTransactionListResponseRepository;
import com.phonepe.payment.staticqr.repository.StaticQRMetadataRequestRepository;
import com.phonepe.payment.staticqr.repository.StaticQRMetadataResponseRepository;
import com.phonepe.payment.util.GenerateXVerifyKey;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.util.PhonePeWebClientFactory;
import com.phonepe.payment.exception.TrackExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing Static QR code payment transactions through PhonePe APIs.
 *
 * <p>This service handles Static QR code-based payment processing, providing capabilities
 * for retrieving transaction lists and metadata for permanent QR codes that can be used
 * for multiple transactions without regeneration.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see StaticQRTransactionListRequest
 * @see StaticQRTransactionListResponse
 */
@Service
@Slf4j
public class StaticQRService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StaticQRTransactionListRequestRepository staticQRTransactionListRequestRepository;
    @Autowired
    private StaticQRTransactionListResponseRepository staticQRTransactionListResponseRepository;
    @Autowired
    private StaticQRMetadataRequestRepository staticQRMetadataRequestRepository;
    @Autowired
    private StaticQRMetadataResponseRepository staticQRMetadataResponseRepository;
    @Autowired
    private TrackExceptionService trackExceptionService;
    @Autowired
    private PhonePeWebClientFactory webClientFactory;

    public String convertToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Value("${static.phonepe.saltKey}")
    private String saltKey;

    @Value("${static.phonepe.saltIndex}")
    private String saltIndex;

    @Value("${static.phonepe.baseUrl}")
    private String baseUrl;

    @Value("${static.phonepe.endpoint}")
    private String endpoint;

    @Value("${static.phonepe.metadataEndpoint}")
    private String metadataEndpoint;

    @Value("${static.phonepe.callback}")
    private String callback;

    @Value("${static.phonepe.numeric}")
    private String numeric;

    public ResponseEntity<?> getTransactionList(StaticQRTransactionListRequest staticQRTransactionListRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_SERVICE_GET_TRANSACTIONS_START")
                .field("merchantId", staticQRTransactionListRequest.getMerchantId())
                .field("storeId", staticQRTransactionListRequest.getStoreId())
                .field("size", staticQRTransactionListRequest.getSize())
                .field("startTimestamp", staticQRTransactionListRequest.getStartTimestamp())
                .timestamp()
                .info("Starting Static QR transaction list retrieval");
            StaticQRTransactionListRequest request = StaticQRTransactionListRequest.builder()
                    .merchantId(staticQRTransactionListRequest.getMerchantId())
                    .provider(staticQRTransactionListRequest.getProvider())
                    .size(staticQRTransactionListRequest.getSize())
                    .storeId(staticQRTransactionListRequest.getStoreId())
                    .startTimestamp(staticQRTransactionListRequest.getStartTimestamp())
                    .createdAt(LocalDateTime.now())
                    .build();

            String jsonBody = objectMapper.writeValueAsString(staticQRTransactionListRequest);
            JSONObject params = new JSONObject(jsonBody);

            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);

            String path = base64EncodedString + endpoint;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            StructuredLogger.Patterns.logPhonepeApiCall(log, "STATIC_QR_GET_TRANSACTIONS", baseUrl + endpoint,
                wrapper, "STATIC_QR_" + System.currentTimeMillis());

            WebClient webClient = webClientFactory.createWebClient();
            long phonepeCallStart = System.currentTimeMillis();
            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + endpoint)
                    .header("X-VERIFY", xVerify)
                    .header("X-CALLBACK-URL", callback)
                    .header("X-CALL-MODE", "POST")
                    .header("Content-Type", "application/json")
                    .header("X-PROVIDER-ID", staticQRTransactionListRequest.getProvider())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(wrapper)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException(
                                    "PhonePe API Error: HTTP " + clientResponse.statusCode() + " - " + errorBody)));
                        }
                        return clientResponse.bodyToMono(String.class);
                    })
            ).block();

            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_GET_TRANSACTIONS_RESPONSE_RECEIVED")
                .field("merchantId", staticQRTransactionListRequest.getMerchantId())
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "STATIC_QR_GET_TRANSACTIONS", baseUrl + endpoint,
                response, System.currentTimeMillis() - phonepeCallStart, 200);

            if (response == null) {
                throw new StaticQRApiException(StaticQRResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            staticQRTransactionListRequestRepository.save(request);
            StaticQRTransactionListResponse staticQRTransactionListResponse = objectMapper.readValue(response, StaticQRTransactionListResponse.class);
            StaticQRResponseCode responseCode = StaticQRResponseCode.fromString(staticQRTransactionListResponse.getCode());

            if (staticQRTransactionListResponse.getData() != null && staticQRTransactionListResponse.getData().getTransactions() != null) {
                for (StaticQRTransactionListResponse.Transaction transaction : staticQRTransactionListResponse.getData().getTransactions()) {
                    if (transaction.getAmount() != null) {
                        transaction.setAmount(transaction.getAmount() / 100);
                    }
                }
            }


            staticQRTransactionListResponseRepository.save(staticQRTransactionListResponse);


            int transactionCount = staticQRTransactionListResponse.getData() != null &&
                staticQRTransactionListResponse.getData().getTransactions() != null ?
                staticQRTransactionListResponse.getData().getTransactions().size() : 0;

            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_SERVICE_GET_TRANSACTIONS_SUCCESS")
                .field("merchantId", staticQRTransactionListRequest.getMerchantId())
                .field("transactionCount", transactionCount)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Static QR transaction list retrieval completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(staticQRTransactionListResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_SERVICE_GET_TRANSACTIONS_JSON_ERROR")
                .field("merchantId", staticQRTransactionListRequest.getMerchantId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during Static QR transaction list retrieval", e);
            throw new StaticQRApiException(StaticQRResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_SERVICE_GET_TRANSACTIONS_WEBCLIENT_ERROR")
                .field("merchantId", staticQRTransactionListRequest.getMerchantId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during Static QR transaction list retrieval", e);
            throw new StaticQRApiException(StaticQRResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (StaticQRApiException e) {
            // Re-throw StaticQRTransactionListApiException as is
            throw e;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "STATIC_QR_SERVICE_GET_TRANSACTIONS_UNEXPECTED_ERROR",
                "STATIC_QR_" + staticQRTransactionListRequest.getMerchantId(),
                "Unexpected error during Static QR transaction list retrieval", e);
            throw new StaticQRApiException(StaticQRResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }



    public ResponseEntity<?> getTransactionMetadata(StaticQRMetadataRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_METADATA_START")
                .field("merchantId", request.getMerchantId())
                .field("phonepeTransactionId", request.getPhonepeTransactionId())
                .field("provider", request.getProvider())
                .field("schemaVersionNumber", request.getSchemaVersionNumber())
                .timestamp()
                .info("Starting Static QR transaction metadata request");

            // Set creation timestamp
            request.setCreatedAt(LocalDateTime.now());

            // Convert request to JSON and encode
            String jsonBody = objectMapper.writeValueAsString(request);
            JSONObject params = new JSONObject(jsonBody);
            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);

            // Generate X-VERIFY header for metadata endpoint
            String path = base64EncodedString + metadataEndpoint;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            // Create request wrapper
            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            StructuredLogger.Patterns.logPhonepeApiCall(log, "STATIC_QR_METADATA",
                baseUrl + metadataEndpoint, wrapper, request.getPhonepeTransactionId());

            // Make API call to PhonePe
            WebClient webClient = webClientFactory.createWebClient();
            long phonepeCallStart = System.currentTimeMillis();

            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + metadataEndpoint)
                    .header("X-VERIFY", xVerify)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-PROVIDER-ID", request.getProvider())
                    .bodyValue(wrapper)
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().isError()) {
                            return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException(
                                    "PhonePe API Error: HTTP " + clientResponse.statusCode() + " - " + errorBody)));
                        }
                        return clientResponse.bodyToMono(String.class);
                    })
            ).block();

            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_METADATA_RESPONSE_RECEIVED")
                .field("merchantId", request.getMerchantId())
                .field("phonepeTransactionId", request.getPhonepeTransactionId())
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "STATIC_QR_METADATA",
                baseUrl + metadataEndpoint, response,
                System.currentTimeMillis() - phonepeCallStart, 200);

            staticQRMetadataRequestRepository.save(request);
            StaticQRMetadataResponse metadataResponse = objectMapper.readValue(response, StaticQRMetadataResponse.class);
            staticQRMetadataResponseRepository.save(metadataResponse);

            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_METADATA_SUCCESS")
                .field("merchantId", request.getMerchantId())
                .field("phonepeTransactionId", request.getPhonepeTransactionId())
                .field("metadataCount", metadataResponse.getData() != null && metadataResponse.getData().getMetadata() != null ? metadataResponse.getData().getMetadata().size() : 0)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Static QR transaction metadata request completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(metadataResponse);

        } catch (JsonProcessingException e) {
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_METADATA_JSON_ERROR")
                .field("merchantId", request.getMerchantId())
                .field("phonepeTransactionId", request.getPhonepeTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during Static QR metadata request", e);
            throw new StaticQRApiException(StaticQRResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            StructuredLogger.forLogger(log)
                .operation("STATIC_QR_METADATA_WEBCLIENT_ERROR")
                .field("merchantId", request.getMerchantId())
                .field("phonepeTransactionId", request.getPhonepeTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during Static QR metadata request", e);
            throw e;
        } catch (Exception e) {
            StructuredLogger.Patterns.logError(log, "STATIC_QR_METADATA_UNEXPECTED_ERROR",
                request.getPhonepeTransactionId(), "Unexpected error during Static QR metadata request", e);
            throw new StaticQRApiException(StaticQRResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }
}

