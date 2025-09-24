package com.phonepe.payment.edc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import com.phonepe.payment.edc.constants.EdcResponseCode;
import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusRequest;
import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusResponse;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionRequest;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionResponse;
import com.phonepe.payment.edc.exception.EdcApiException;
import com.phonepe.payment.edc.repository.EdcCheckTransactionStatusRequestRepository;
import com.phonepe.payment.edc.repository.EdcCheckTransactionStatusResponseRepository;
import com.phonepe.payment.edc.repository.EdcInitializeTransactionRequestRepository;
import com.phonepe.payment.edc.repository.EdcInitializeTransactionResponseRepository;
import com.phonepe.payment.util.GenerateTransactionId;
import com.phonepe.payment.util.GenerateXVerifyKey;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.util.PhonePeWebClientFactory;
import com.phonepe.payment.exception.TrackExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing Electronic Data Capture (EDC) payment transactions through PhonePe APIs.
 *
 * <p>This service handles EDC-based payment processing for point-of-sale terminal
 * integrations, providing transaction initialization and status checking capabilities
 * for card and UPI payments through EDC terminals.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see EdcInitializeTransactionRequest
 * @see EdcInitializeTransactionResponse
 */
@Service
@Slf4j
public class EdcService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EdcInitializeTransactionRequestRepository edcInitializeTransactionRequestRepository;
    @Autowired
    private EdcInitializeTransactionResponseRepository edcInitializeTransactionResponseRepository;
    @Autowired
    private EdcCheckTransactionStatusRequestRepository edcCheckTransactionStatusRequestRepository;
    @Autowired
    private EdcCheckTransactionStatusResponseRepository edcCheckTransactionStatusResponseRepository;
    @Autowired
    private TrackExceptionService trackExceptionService;
    @Autowired
    private PhonePeWebClientFactory webClientFactory;

    public String convertToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Value("${edc.phonepe.numeric}")
    private String numeric;

    @Value("${edc.phonepe.saltKey}")
    private String saltKey;

    @Value("${edc.phonepe.saltIndex}")
    private String saltIndex;

    @Value("${edc.phonepe.baseUrl}")
    private String baseUrl;

    @Value("${edc.phonepe.endpoint}")
    private String endpoint;

    @Value("${edc.phonepe.expiresIn}")
    private String expiresIn;

    @Value("${edc.phonepe.callback}")
    private String callback;

    @Value("${edc.phonepe.checkPaymentStatusEndPoint}")
    private String checkPaymentStatusEndPoint;

    @Value("${edc.phonepe.checkPaymentStatusEndPointPath}")
    private String checkPaymentStatusEndPointPath;


    public ResponseEntity<?> initTransaction(EdcInitializeTransactionRequest edcInitializeTransactionRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;

        try {
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_INIT_START")
                .transactionId(edcInitializeTransactionRequest.getTransactionId())
                .field("merchantId", edcInitializeTransactionRequest.getMerchantId())
                .amount(edcInitializeTransactionRequest.getAmount())
                .field("storeId", edcInitializeTransactionRequest.getStoreId())
                .field("terminalId", edcInitializeTransactionRequest.getTerminalId())
                .timestamp()
                .info("Starting EDC transaction initialization");
            transactionId = GenerateTransactionId.generateTransactionId(numeric);
            EdcInitializeTransactionRequest request = EdcInitializeTransactionRequest.builder()
                    .merchantId(edcInitializeTransactionRequest.getMerchantId())
                    .storeId(edcInitializeTransactionRequest.getStoreId())
                    .orderId(transactionId)
                    .transactionId(transactionId)
                    .amount(edcInitializeTransactionRequest.getAmount()*100)
                    .terminalId(edcInitializeTransactionRequest.getTerminalId())
                    .integrationMappingType("ONE_TO_ONE")
                    .paymentModes(Arrays.asList("CARD", "DQR"))
                    .timeAllowedForHandoverToTerminalSeconds(60)
                    .createdAt(LocalDateTime.now()).build();

            String jsonBody = objectMapper.writeValueAsString(request);
            JSONObject params = new JSONObject(jsonBody);

            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);

            JSONObject params1 = new JSONObject();
            params1.put("request", base64EncodedString);

            String path = base64EncodedString + endpoint;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            StructuredLogger.Patterns.logPhonepeApiCall(log, "EDC_INIT", baseUrl + endpoint,
                wrapper, transactionId);

            WebClient webClient = webClientFactory.createWebClient();
            long phonepeCallStart = System.currentTimeMillis();
            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + endpoint)
                    .header("X-VERIFY", xVerify)
                    .header("X-CALLBACK-URL", callback)
                    .header("X-PROVIDER-ID", edcInitializeTransactionRequest.getProvider())
                    .header("Content-Type", "application/json")
                    .header("X-CALL-MODE", "POST")
                    .contentType(MediaType.APPLICATION_JSON).bodyValue(wrapper)
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
                .operation("EDC_INIT_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .responseTime(System.currentTimeMillis() - phonepeCallStart)
                .timestamp()
                .info("Raw PhonePe API response received");

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "EDC_INIT", baseUrl + endpoint,
                response, System.currentTimeMillis() - phonepeCallStart, 200);

            edcInitializeTransactionRequestRepository.save(request);

            if (response == null) {
                throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            EdcInitializeTransactionResponse edcInitializeTransactionResponse = objectMapper.readValue(response, EdcInitializeTransactionResponse.class);
            EdcResponseCode responseCode = EdcResponseCode.fromString(edcInitializeTransactionResponse.getCode());


            if (edcInitializeTransactionResponse.getData() != null && edcInitializeTransactionResponse.getData().getAmount() != null) {
                edcInitializeTransactionResponse.getData().setAmount(edcInitializeTransactionResponse.getData().getAmount() / 100);
            }

            edcInitializeTransactionResponseRepository.save(edcInitializeTransactionResponse);

            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_INIT_SUCCESS")
                .transactionId(transactionId)
                .field("edcTransactionId", edcInitializeTransactionResponse.getData().getTransactionId())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("EDC transaction initialization completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(edcInitializeTransactionResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_INIT_JSON_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during EDC initialization", e);
            throw new EdcApiException(EdcResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_INIT_WEBCLIENT_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during EDC initialization", e);
            throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (EdcApiException e) {
            throw e;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "EDC_SERVICE_INIT_UNEXPECTED_ERROR", transactionId,
                "Unexpected error during EDC transaction initialization", e);
            throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> checkPaymentStatus(EdcCheckTransactionStatusRequest request) throws JsonProcessingException {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_STATUS_CHECK_START")
                .transactionId(request.getTransactionId())
                .field("merchantId", request.getMerchantId())
                .timestamp()
                .info("Starting EDC payment status check");
            String path = checkPaymentStatusEndPoint + request.getMerchantId() + "/" + request.getTransactionId() + checkPaymentStatusEndPointPath;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
            long phonepeCallStart = System.currentTimeMillis();
            WebClient webClient = webClientFactory.createWebClient();
            var response = webClientFactory.withRetry(
                webClient.get().uri(baseUrl + path)
                    .header("X-VERIFY", xVerify)
                    .header("X-CALLBACK-URL", callback)
                    .header("X-PROVIDER-ID", request.getProvider())
                    .header("X-MERCHANT-ID", request.getMerchantId())
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

            StructuredLogger.forLogger(log)
                    .operation("EDC_CHECK_STATUS_RESPONSE_RECEIVED")
                    .field("rawResponse", response)
                    .field("responseLength", response != null ? response.length() : 0)
                    .responseTime(System.currentTimeMillis() - phonepeCallStart)
                    .timestamp()
                    .info("Raw PhonePe API response received");

            EdcCheckTransactionStatusRequest tx = EdcCheckTransactionStatusRequest
                    .builder()
                    .transactionId(request.getTransactionId())
                    .merchantId(request.getMerchantId())
                    .checkedStatusAt(LocalDateTime.now()).build();
            edcCheckTransactionStatusRequestRepository.save(tx);
            EdcCheckTransactionStatusResponse edcCheckTransactionStatusResponse = objectMapper.readValue(response, EdcCheckTransactionStatusResponse.class);

            if (edcCheckTransactionStatusResponse.getData() != null && edcCheckTransactionStatusResponse.getData().getAmount() != null) {
                edcCheckTransactionStatusResponse.getData().setAmount(edcCheckTransactionStatusResponse.getData().getAmount() / 100);
            }

            edcCheckTransactionStatusResponseRepository.save(edcCheckTransactionStatusResponse);

            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_STATUS_CHECK_SUCCESS")
                .transactionId(request.getTransactionId())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("EDC payment status check completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(edcCheckTransactionStatusResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_STATUS_CHECK_JSON_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during EDC status check", e);
            throw new EdcApiException(EdcResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("EDC_SERVICE_STATUS_CHECK_WEBCLIENT_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during EDC status check", e);
            throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (EdcApiException e) {
            throw e;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "EDC_SERVICE_STATUS_CHECK_UNEXPECTED_ERROR",
                request.getTransactionId(), "Unexpected error during EDC payment status check", e);
            throw new EdcApiException(EdcResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

}
