package com.phonepe.payment.paymentlink.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.exception.TrackExceptionService;
import reactor.core.publisher.Mono;
import com.phonepe.payment.paymentlink.constants.PaymentLinkResponseCode;
import com.phonepe.payment.paymentlink.entity.*;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCancelTransactionResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkCheckTransactionStatusResponse;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionRequest;
import com.phonepe.payment.paymentlink.entity.PaymentLinkRefundTransactionResponse;
import com.phonepe.payment.paymentlink.exception.PaymentLinkApiException;
import com.phonepe.payment.paymentlink.repository.*;
import com.phonepe.payment.util.GenerateTransactionId;
import com.phonepe.payment.util.GenerateXVerifyKey;
import com.phonepe.payment.util.PhonePeWebClientFactory;
import com.phonepe.payment.util.StructuredLogger;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service class for managing Payment Link transactions through PhonePe APIs.
 *
 * <p>This service handles Payment Link-based payment processing, providing capabilities
 * for creating payment links, managing transaction lifecycle, and processing payments
 * through web-based interfaces accessible via URL links.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see PaymentLinkRequest
 * @see PaymentLinkResponse
 */
@Service
@Slf4j
public class PaymentLinkService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentLinkRequestRepository paymentLinkRequestRepository;
    @Autowired
    private PaymentLinkResponseRepository paymentLinkResponseRepository;
    @Autowired
    private TrackExceptionService trackExceptionService;
    @Autowired
    private PhonePeWebClientFactory webClientFactory;
    @Autowired
    private PaymentLinkCancelTransactionRequestRepository cancelRequestRepository;
    @Autowired
    private PaymentLinkCancelTransactionResponseRepository cancelResponseRepository;
    @Autowired
    private PaymentLinkCheckTransactionStatusRequestRepository statusRequestRepository;
    @Autowired
    private PaymentLinkCheckTransactionStatusResponseRepository statusResponseRepository;
    @Autowired
    private PaymentLinkRefundTransactionRequestRepository refundRequestRepository;
    @Autowired
    private PaymentLinkRefundTransactionResponseRepository refundResponseRepository;

    public String convertToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private String merchantOrderId;

    @Value("${payment.phonepe.saltKey}")
    private String saltKey;

    @Value("${payment.phonepe.saltIndex}")
    private String saltIndex;

    @Value("${payment.phonepe.baseUrl}")
    private String baseUrl;

    @Value("${payment.phonepe.endpoint}")
    private String endpoint;

    @Value("${payment.phonepe.expiresIn}")
    private String expiresIn;

    @Value("${payment.phonepe.callback}")
    private String callback;

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

    @Value("${payment.phonepe.numeric}")
    private String numeric;

    public ResponseEntity<?> initTransaction(PaymentLinkRequest paymentLinkRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;

        try {
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_SERVICE_INIT_START")
                .transactionId(paymentLinkRequest.getTransactionId())
                .field("merchantId", paymentLinkRequest.getMerchantId())
                .amount(paymentLinkRequest.getAmount())
                .field("mobileNumber", paymentLinkRequest.getMobileNumber())
                .field("shortName", paymentLinkRequest.getShortName())
                .timestamp()
                .info("Starting Payment Link transaction initialization");
            transactionId = GenerateTransactionId.generateTransactionId(numeric);
            PaymentLinkRequest request = PaymentLinkRequest.builder()
                    .merchantId(paymentLinkRequest.getMerchantId())
                    .transactionId(transactionId)
                    .merchantOrderId(transactionId)
                    .storeId(paymentLinkRequest.getStoreId())
                    .terminalId(paymentLinkRequest.getTerminalId())
                    .amount(paymentLinkRequest.getAmount())
                    .mobileNumber(paymentLinkRequest.getMobileNumber())
                    .message(paymentLinkRequest.getMessage())
                    .expiresIn(paymentLinkRequest.getExpiresIn())
                    .shortName(paymentLinkRequest.getShortName())
                    .subMerchantId(paymentLinkRequest.getSubMerchantId())
                    .createdAt(LocalDateTime.now()).build();
            request.setAmount(request.getAmount()*100);
            String jsonBody = objectMapper.writeValueAsString(request);
            JSONObject params = new JSONObject(jsonBody);
            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);

            JSONObject params1 = new JSONObject();
            params1.put("request", base64EncodedString);

            String path = base64EncodedString + endpoint ;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_REQUEST_PREPARED")
                .transactionId(transactionId)
                .field("base64PayloadLength", base64EncodedString.length())
                .field("endpoint", endpoint)
                .timestamp()
                .debug("Payment link request payload prepared");

            StructuredLogger.Patterns.logPhonepeApiCall(log, "PAYMENT_LINK_INIT", baseUrl + endpoint,
                wrapper, transactionId);

            WebClient webClient = webClientFactory.createWebClient();
            long phonepeCallStart = System.currentTimeMillis();
            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + endpoint)
                    .header("X-VERIFY", xVerify)
                    .header("X-PROVIDER-ID", paymentLinkRequest.getProvider())
                    .header("Content-Type", "application/json")
                    .header("X-CALL-MODE", "POST")
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

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_INIT_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "PAYMENT_LINK_INIT", baseUrl + endpoint,
                response, System.currentTimeMillis() - phonepeCallStart, 200);

            if (response == null) {
                throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            paymentLinkRequestRepository.save(request);
            PaymentLinkResponse paymentLinkResponse = objectMapper.readValue(response, PaymentLinkResponse.class);
            PaymentLinkResponseCode responseCode = PaymentLinkResponseCode.fromString(paymentLinkResponse.getCode());

            if (paymentLinkResponse.getData() != null && paymentLinkResponse.getData().getAmount() != null) {
                paymentLinkResponse.getData().setAmount(paymentLinkResponse.getData().getAmount() / 100);
            }

            paymentLinkResponseRepository.save(paymentLinkResponse);

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_SERVICE_INIT_SUCCESS")
                .transactionId(transactionId)
                .field("paymentLinkUrl", paymentLinkResponse.getData().getPayLink())
                .field("upiIntent", paymentLinkResponse.getData().getUpiIntent())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Payment Link transaction initialization completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(paymentLinkResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_SERVICE_INIT_JSON_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during Payment Link initialization", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_SERVICE_INIT_WEBCLIENT_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during Payment Link initialization", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (PaymentLinkApiException e) {
            // Re-throw PaymentLinkApiException as is
            throw e;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_SERVICE_INIT_UNEXPECTED_ERROR", transactionId,
                "Unexpected error during Payment Link transaction initialization", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Cancels a payment link transaction.
     *
     * @param cancelRequest the cancel transaction request containing merchantId, transactionId, and cancellation details
     * @return ResponseEntity containing the cancellation response from PhonePe API
     * @throws PaymentLinkApiException if cancellation fails or API returns error
     */
    public ResponseEntity<?> cancelTransaction(PaymentLinkCancelTransactionRequest cancelRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = cancelRequest.getTransactionId();

        try {
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_CANCEL_START")
                .transactionId(transactionId)
                .field("merchantId", cancelRequest.getMerchantId())
                .timestamp()
                .info("Starting Payment Link transaction cancellation");

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

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_CANCEL_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            if (response == null) {
                throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            cancelRequest.setCreatedAt(LocalDateTime.now());
            cancelRequestRepository.save(cancelRequest);
            PaymentLinkCancelTransactionResponse cancelResponse = objectMapper.readValue(response, PaymentLinkCancelTransactionResponse.class);
            PaymentLinkResponseCode responseCode = PaymentLinkResponseCode.fromString(cancelResponse.getCode());
            cancelResponseRepository.save(cancelResponse);

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_CANCEL_SUCCESS")
                .transactionId(transactionId)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Payment Link transaction cancellation completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(cancelResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_CANCEL_JSON_ERROR")
                    .transactionId(cancelRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during PAYMENT_LINK cancellation", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_CANCEL_WEBCLIENT_ERROR")
                    .transactionId(cancelRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during PAYMENT_LINK cancellation", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_SERVICE_CANCEL_UNEXPECTED_ERROR",
                    cancelRequest.getTransactionId(), "Unexpected error during PAYMENT_LINK transaction cancellation", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Checks the status of a payment link transaction.
     *
     * @param statusRequest the status check request containing merchantId and transactionId
     * @return ResponseEntity containing the transaction status response from PhonePe API
     * @throws PaymentLinkApiException if status check fails or API returns error
     */
    public ResponseEntity<?> checkPaymentStatus(PaymentLinkCheckTransactionStatusRequest statusRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = statusRequest.getTransactionId();

        try {
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_STATUS_CHECK_START")
                .transactionId(transactionId)
                .field("merchantId", statusRequest.getMerchantId())
                .timestamp()
                .info("Starting Payment Link transaction status check");

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

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_STATUS_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            if (response == null) {
                throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            statusRequest.setCreatedAt(LocalDateTime.now());
            statusRequestRepository.save(statusRequest);
            PaymentLinkCheckTransactionStatusResponse statusResponse = objectMapper.readValue(response, PaymentLinkCheckTransactionStatusResponse.class);
            PaymentLinkResponseCode responseCode = PaymentLinkResponseCode.fromString(statusResponse.getCode());

            if (statusResponse.getData() != null && statusResponse.getData().getAmount() != null) {
                statusResponse.getData().setAmount(statusResponse.getData().getAmount() / 100);
            }

            statusResponseRepository.save(statusResponse);

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_STATUS_CHECK_SUCCESS")
                .transactionId(transactionId)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Payment Link transaction status check completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(statusResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_STATUS_CHECK_JSON_ERROR")
                    .transactionId(statusRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during PAYMENT_LINK status check", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_STATUS_CHECK_WEBCLIENT_ERROR")
                    .transactionId(statusRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during PAYMENT_LINK status check", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (Exception e) {
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_SERVICE_STATUS_CHECK_UNEXPECTED_ERROR",
                    statusRequest.getTransactionId(), "Unexpected error during PAYMENT_LINK payment status check", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Processes a refund for a payment link transaction.
     *
     * @param refundRequest the refund request containing transactionId, refundAmount, and merchant details
     * @return ResponseEntity containing the refund response from PhonePe API
     * @throws PaymentLinkApiException if refund fails or API returns error
     */
    public ResponseEntity<?> refundTransaction(PaymentLinkRefundTransactionRequest refundRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;

        try {
            transactionId = GenerateTransactionId.generateTransactionId(numeric);
            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_REFUND_START")
                .transactionId(transactionId)
                .field("merchantId", refundRequest.getMerchantId())
                .amount(refundRequest.getRefundAmount())
                .timestamp()
                .info("Starting Payment Link transaction refund");


            refundRequest.setAmount(refundRequest.getAmount()*100);
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

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_REFUND_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            if (response == null) {
                throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            refundRequestRepository.save(refundRequest);
            PaymentLinkRefundTransactionResponse refundResponse = objectMapper.readValue(response, PaymentLinkRefundTransactionResponse.class);
            PaymentLinkResponseCode responseCode = PaymentLinkResponseCode.fromString(refundResponse.getCode());

            if (refundResponse.getAmount() > 0) {
                refundResponse.setAmount(refundResponse.getAmount() / 100);
            }

            refundResponseRepository.save(refundResponse);

            StructuredLogger.forLogger(log)
                .operation("PAYMENT_LINK_REFUND_SUCCESS")
                .transactionId(transactionId)
                .amount(refundRequest.getRefundAmount())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("Payment Link transaction refund completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(refundResponse);
        }catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_REFUND_JSON_ERROR")
                    .transactionId(refundRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("JSON processing failed during PAYMENT_LINK refund", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                    .operation("PAYMENT_LINK_SERVICE_REFUND_WEBCLIENT_ERROR")
                    .transactionId(refundRequest.getTransactionId())
                    .error(e)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .error("PhonePe API call failed during PAYMENT_LINK refund", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        }  catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.Patterns.logError(log, "PAYMENT_LINK_SERVICE_REFUND_UNEXPECTED_ERROR",
                    refundRequest.getTransactionId(), "Unexpected error during PAYMENT_LINK transaction refund", e);
            throw new PaymentLinkApiException(PaymentLinkResponseCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }
}
