package com.phonepe.payment.collect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.collect.constants.CollectResponseCode;
import com.phonepe.payment.collect.entity.*;
import com.phonepe.payment.collect.exception.CollectApiException;
import com.phonepe.payment.collect.repository.*;
import com.phonepe.payment.util.GenerateTransactionId;
import com.phonepe.payment.util.GenerateXVerifyKey;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.util.PhonePeWebClientFactory;
import com.phonepe.payment.util.CommonServiceUtils;
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

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for managing PhonePe Collect Call transactions.
 *
 * <p>This service handles collect call payment processing through PhonePe APIs,
 * providing capabilities for initiating collect calls to customer mobile numbers
 * and managing the complete transaction lifecycle.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @see CollectCallRequest
 * @see CollectCallResponse
 * @since 1.0
 */
@Service
@Slf4j
public class CollectService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollectCallRequestRepository collectCallRequestRepository;
    @Autowired
    private CollectCallResponseRepository collectCallResponseRepository;
    @Autowired
    private TrackExceptionService trackExceptionService;
    @Autowired
    private PhonePeWebClientFactory webClientFactory;
    @Autowired
    private CommonServiceUtils commonServiceUtils;
    @Autowired
    private CollectCallCancelTransactionRequestRepository cancelRequestRepository;
    @Autowired
    private CollectCallCancelTransactionResponseRepository cancelResponseRepository;
    @Autowired
    private CollectCallCheckTransactionStatusRequestRepository statusRequestRepository;
    @Autowired
    private CollectCallCheckTransactionStatusResponseRepository statusResponseRepository;
    @Autowired
    private CollectCallRefundTransactionRequestRepository refundRequestRepository;
    @Autowired
    private CollectCallRefundTransactionResponseRepository refundResponseRepository;

    public String convertToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }


    @Value("${collect.phonepe.saltKey}")
    private String saltKey;

    @Value("${collect.phonepe.saltIndex}")
    private String saltIndex;

    @Value("${collect.phonepe.baseUrl}")
    private String baseUrl;

    @Value("${collect.phonepe.endpoint}")
    private String endpoint;

    @Value("${collect.phonepe.callback}")
    private String callback;

    @Value("${collect.phonepe.numeric}")
    private String numeric;


    @Value("${dqr.phonepe.refundEndPoint}")
    private String refundEndPoint;

    @Value("${dqr.phonepe.cancelEndPoint}")
    private String cancelEndPoint;

    @Value("${dqr.phonepe.cancelEndPointPath}")
    private String cancelEndPointPath;

    @Value("${dqr.phonepe.checkPaymentStatusEndPoint}")
    private String checkPaymentStatusEndPoint;

    @Value("${dqr.phonepe.checkPaymentStatusEndPointPath}")
    private String checkPaymentStatusEndPointPath;

    public ResponseEntity<?> initiateCollectCall(CollectCallRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            String transactionId = GenerateTransactionId.generateTransactionId(numeric);
            request.setTransactionId(transactionId);
            request.setMerchantOrderId(transactionId);
            request.setAmount(request.getAmount() * 100); // Convert to paise
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_START")
                    .field("merchantId", request.getMerchantId())
                    .field("transactionId", transactionId)
                    .field("amount", request.getAmount())
                    .timestamp()
                    .info("Starting collect call transaction");

            String jsonBody = objectMapper.writeValueAsString(request);
            JSONObject params = new JSONObject(jsonBody);

            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);
            String path = base64EncodedString + endpoint;
            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            String xVerify;
            try {
                xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
            } catch (Exception e) {
                StructuredLogger.forLogger(log)
                        .operation("COLLECT_CALL_X_VERIFY_ERROR")
                        .field("merchantId", request.getMerchantId())
                        .field("transactionId", request.getTransactionId())
                        .error(e)
                        .timestamp()
                        .error("Failed to generate X-VERIFY key for collect call", e);
                throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "Failed to generate authentication key");
            }

            long phonepeCallStart = System.currentTimeMillis();
            WebClient webClient = webClientFactory.createWebClient();

            var response = webClientFactory.withRetry(
                    webClient.post().uri(baseUrl + endpoint)
                            .header("X-VERIFY", xVerify)
                            .header("X-CALLBACK-URL", callback)
                            .header("Content-Type", "application/json")
                            .header("X-CALL-MODE", "POST")
                            .header("X-PROVIDER-ID", request.getProvider())
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

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "COLLECT_CALL",
                    baseUrl + endpoint, response,
                    System.currentTimeMillis() - phonepeCallStart, 200);

            collectCallRequestRepository.save(request);
            CollectCallResponse collectResponse = objectMapper.readValue(response, CollectCallResponse.class);
            if (collectResponse.getData() != null && collectResponse.getData().getAmount() != null) {
                collectResponse.getData().setAmount(collectResponse.getData().getAmount() / 100);
            }
            collectCallResponseRepository.save(collectResponse);

            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SUCCESS")
                    .field("merchantId", request.getMerchantId())
                    .field("transactionId", request.getTransactionId())
                    .field("responseTransactionId", collectResponse.getData() != null ? collectResponse.getData().getTransactionId() : "N/A")
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .info("Collect call transaction completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(collectResponse);

        } catch (CollectApiException e) {
            throw e;
        } catch (Exception e) {
            commonServiceUtils.handleCollectApiError(e, request.getTransactionId(), "INITIATE", log);
            return null; // This line will never be reached as handleCollectApiError always throws
        }
    }

    /**
     * Cancels a Collect Call transaction.
     *
     * @param cancelRequest the cancel transaction request containing merchantId, transactionId, and cancellation details
     * @return ResponseEntity containing the cancellation response from PhonePe API
     * @throws CollectApiException if cancellation fails or API returns error
     */
    public ResponseEntity<?> cancelTransaction(CollectCallCancelTransactionRequest cancelRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = cancelRequest.getTransactionId();

        try {
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_CANCEL_START")
                    .transactionId(transactionId)
                    .field("merchantId", cancelRequest.getMerchantId())
                    .timestamp()
                    .info("Starting Collect Call transaction cancellation");

            String path = cancelEndPoint + cancelRequest.getMerchantId() + "/" + cancelRequest.getTransactionId() + cancelEndPointPath;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            WebClient webClient = webClientFactory.createWebClient();
            var response = webClientFactory.withRetry(
                    webClient.post().uri(baseUrl + path)
                            .header("X-VERIFY", xVerify)
                            .header("Content-Type", "application/json")
                            .header("X-PROVIDER-ID", cancelRequest.getProvider())
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(cancelRequest)
                            .exchangeToMono(clientResponse -> {
                                if (clientResponse.statusCode().isError()) {
                                    return clientResponse.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(new RuntimeException(
                                                    "PhonePe API Error: HTTP " + clientResponse.statusCode() + " - " + errorBody)));
                                }
                                log.info("Status code initTransaction: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class);
                            })
            ).block();

            if (response == null) {
                throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            cancelRequest.setCreatedAt(LocalDateTime.now());
            cancelRequestRepository.save(cancelRequest);
            CollectCallCancelTransactionResponse cancelResponse = objectMapper.readValue(response, CollectCallCancelTransactionResponse.class);
            CollectResponseCode responseCode = CollectResponseCode.fromString(cancelResponse.getCode());
            cancelResponseRepository.save(cancelResponse);

            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_CANCEL_SUCCESS")
                    .transactionId(transactionId)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .info("Collect Call transaction cancellation completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(cancelResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_CANCEL_JSON_ERROR")
                    .transactionId(cancelRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during COLLECT_CALL cancellation", e);
            throw new CollectApiException(CollectResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_CANCEL_WEBCLIENT_ERROR")
                    .transactionId(cancelRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during COLLECT_CALL cancellation", e);
            throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (Exception e) {
            commonServiceUtils.handleCollectApiError(e, cancelRequest.getTransactionId(), "CANCEL", log);
            return null; // This line will never be reached as handleCollectApiError always throws
        }
    }

    /**
     * Checks the status of a Collect Call transaction.
     *
     * @param statusRequest the status check request containing merchantId and transactionId
     * @return ResponseEntity containing the transaction status response from PhonePe API
     * @throws CollectApiException if status check fails or API returns error
     */
    public ResponseEntity<?> checkPaymentStatus(CollectCallCheckTransactionStatusRequest statusRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = statusRequest.getTransactionId();

        try {
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_STATUS_CHECK_START")
                    .transactionId(transactionId)
                    .field("merchantId", statusRequest.getMerchantId())
                    .timestamp()
                    .info("Starting Collect Call transaction status check");

            String path = checkPaymentStatusEndPoint + statusRequest.getMerchantId() + "/" + statusRequest.getTransactionId() + checkPaymentStatusEndPointPath;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
            WebClient webClient = webClientFactory.createWebClient();
            var response = webClientFactory.withRetry(
                    webClient.get().uri(baseUrl + path)
                            .header("X-VERIFY", xVerify)
                            .header("X-CALLBACK-URL", callback)
                            .header("X-PROVIDER-ID", statusRequest.getProvider())
                            .header("X-MERCHANT-ID", statusRequest.getMerchantId())
                            .header("Content-Type", "application/json")
                            .exchangeToMono(clientResponse -> {
                                if (clientResponse.statusCode().isError()) {
                                    return clientResponse.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(new RuntimeException(
                                                    "PhonePe API Error: HTTP " + clientResponse.statusCode() + " - " + errorBody)));
                                }
                                return clientResponse.bodyToMono(String.class);
                            })
            ).block();

            if (response == null) {
                throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            statusRequest.setCreatedAt(LocalDateTime.now());
            statusRequestRepository.save(statusRequest);
            CollectCallCheckTransactionStatusResponse statusResponse = objectMapper.readValue(response, CollectCallCheckTransactionStatusResponse.class);
            CollectResponseCode responseCode = CollectResponseCode.fromString(statusResponse.getCode());

            if (statusResponse.getData() != null && statusResponse.getData().getAmount() != null) {
                statusResponse.getData().setAmount(statusResponse.getData().getAmount() / 100);
            }

            statusResponseRepository.save(statusResponse);

            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_STATUS_CHECK_SUCCESS")
                    .transactionId(transactionId)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .info("Collect Call transaction status check completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(statusResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_STATUS_CHECK_JSON_ERROR")
                    .transactionId(statusRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during COLLECT_CALL status check", e);
            throw new CollectApiException(CollectResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_STATUS_CHECK_WEBCLIENT_ERROR")
                    .transactionId(statusRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during COLLECT_CALL status check", e);
            throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (Exception e) {
            commonServiceUtils.handleCollectApiError(e, statusRequest.getTransactionId(), "STATUS_CHECK", log);
            return null; // This line will never be reached as handleCollectApiError always throws
        }
    }

    /**
     * Processes a refund for a Collect Call transaction.
     *
     * @param refundRequest the refund request containing transactionId, refundAmount, and merchant details
     * @return ResponseEntity containing the refund response from PhonePe API
     * @throws CollectApiException if refund fails or API returns error
     */
    public ResponseEntity<?> refundTransaction(CollectCallRefundTransactionRequest refundRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;

        try {
            transactionId = GenerateTransactionId.generateTransactionId(numeric);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_REFUND_START")
                    .transactionId(transactionId)
                    .field("merchantId", refundRequest.getMerchantId())
                    .amount(refundRequest.getRefundAmount())
                    .timestamp()
                    .info("Starting Collect Call transaction refund");


            refundRequest.setAmount(refundRequest.getAmount() * 100);
            String jsonPayload = objectMapper.writeValueAsString(refundRequest);
            String base64Payload = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
            String path = base64Payload + refundEndPoint;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64Payload);
            WebClient webClient = webClientFactory.createWebClient();
            var response = webClientFactory.withRetry(
                    webClient.post().uri(baseUrl + refundEndPoint)
                            .header("X-VERIFY", xVerify)
                            .header("X-CALLBACK-URL", callback)
                            .header("X-PROVIDER-ID", refundRequest.getProvider())
                            .header("Content-Type", "application/json")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(wrapper).exchangeToMono(clientResponse -> {
                                if (clientResponse.statusCode().isError()) {
                                    return clientResponse.bodyToMono(String.class)
                                            .flatMap(errorBody -> Mono.error(new RuntimeException(
                                                    "PhonePe API Error: HTTP " + clientResponse.statusCode() + " - " + errorBody)));
                                }
                                return clientResponse.bodyToMono(String.class);
                            })
            ).block();

            if (response == null) {
                throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            refundRequestRepository.save(refundRequest);
            CollectCallRefundTransactionResponse refundResponse = objectMapper.readValue(response, CollectCallRefundTransactionResponse.class);
            CollectResponseCode responseCode = CollectResponseCode.fromString(refundResponse.getCode());

            if (refundResponse.getAmount() > 0) {
                refundResponse.setAmount(refundResponse.getAmount() / 100);
            }

            refundResponseRepository.save(refundResponse);

            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_REFUND_SUCCESS")
                    .transactionId(transactionId)
                    .amount(refundRequest.getRefundAmount())
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .info("Collect Call transaction refund completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(refundResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_REFUND_JSON_ERROR")
                    .transactionId(refundRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during COLLECT_CALL refund", e);
            throw new CollectApiException(CollectResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("COLLECT_CALL_SERVICE_REFUND_WEBCLIENT_ERROR")
                    .transactionId(refundRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during COLLECT_CALL refund", e);
            throw new CollectApiException(CollectResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (Exception e) {
            commonServiceUtils.handleCollectApiError(e, refundRequest.getTransactionId(), "REFUND", log);
            return null; // This line will never be reached as handleCollectApiError always throws
        }
    }
}