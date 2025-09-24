package com.phonepe.payment.dqr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClientException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.phonepe.payment.dqr.constants.DqrResponseCode;
import com.phonepe.payment.dqr.entity.*;
import com.phonepe.payment.dqr.exception.DqrApiException;
import com.phonepe.payment.dqr.repository.*;
import com.phonepe.payment.exception.TrackExceptionService;
import com.phonepe.payment.util.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service class for managing Dynamic QR (DQR) payment transactions through PhonePe APIs.
 *
 * <p>This service handles the complete lifecycle of DQR transactions including
 * initialization, cancellation, refunds, and status checks. It provides QR code
 * generation capabilities and manages all interactions with PhonePe's payment gateway.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see DqrInitializeTransactionRequest
 * @see DqrInitializeTransactionResponse
 * @see PhonePeWebClientFactory
 * @see StructuredLogger
 */
@Service
@Slf4j
public class DqrService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DqrInitializeTransactionRequestRepository dqrInitializeTransactionRequestRepository;
    @Autowired
    private DqrCancelTransactionRequestRepository dqrCancelTransactionRequestRepository;
    @Autowired
    private DqrRefundTransactionRequestRepository dqrRefundTransactionRequestRepository;
    @Autowired
    private DqrCheckTransactionStatusRequestRepository dqrCheckTransactionStatusRequestRepository;

    @Autowired
    private DqrInitializeTransactionResponseRepository dqrInitializeTransactionResponseRepository;
    @Autowired
    private DqrCancelTransactionResponseRepository dqrCancelTransactionResponseRepository;
    @Autowired
    private DqrRefundTransactionResponseRepository dqrRefundTransactionResponseRepository;
    @Autowired
    private DqrCheckTransactionStatusResponseRepository dqrCheckTransactionStatusResponseRepository;
    @Autowired
    private TrackExceptionService trackExceptionService;
    @Autowired
    private PhonePeWebClientFactory webClientFactory;
    @Autowired
    private CommonServiceUtils commonServiceUtils;

    @Value("${dqr.phonepe.saltKey}")
    private String saltKey;

    @Value("${dqr.phonepe.saltIndex}")
    private String saltIndex;

    @Value("${dqr.phonepe.baseUrl}")
    private String baseUrl;

    @Value("${dqr.phonepe.endpoint}")
    private String endpoint;

    @Value("${dqr.phonepe.expiresIn}")
    private String expiresIn;

    @Value("${dqr.phonepe.callback}")
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

    /**
     * Initializes a new Dynamic QR (DQR) payment transaction with PhonePe.
     */
    public ResponseEntity<?> initTransaction(DqrInitializeTransactionRequest dqrInitializeTransactionRequest) {
        long startTime = System.currentTimeMillis();
        String transactionId = null;

        try {
            transactionId = GenerateTransactionId.generateTransactionId(numeric);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_INIT_START")
                .transactionId(dqrInitializeTransactionRequest.getTransactionId())
                .field("merchantId", dqrInitializeTransactionRequest.getMerchantId())
                .amount(dqrInitializeTransactionRequest.getAmount())
                .timestamp()
                .info("Starting DQR transaction initialization");

            DqrInitializeTransactionRequest request = DqrInitializeTransactionRequest.builder()
                    .merchantId(dqrInitializeTransactionRequest.getMerchantId())
                    .transactionId(transactionId)
                    .merchantOrderId(transactionId)
                    .amount(dqrInitializeTransactionRequest.getAmount()*100)
                    .expiresIn(Integer.parseInt(expiresIn))
                    .storeId(dqrInitializeTransactionRequest.getStoreId())
                    .terminalId(dqrInitializeTransactionRequest.getTerminalId())
                    .createdAt(LocalDateTime.now()).build();

            String jsonBody = objectMapper.writeValueAsString(request);
            JSONObject params = new JSONObject(jsonBody);

            String jsonString = params.toString();
            byte[] encodedBytes = Base64.getEncoder().encode(jsonString.getBytes());
            String base64EncodedString = new String(encodedBytes);

            String path = base64EncodedString + endpoint;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            Map<String, String> wrapper = new HashMap<>();
            wrapper.put("request", base64EncodedString);

            StructuredLogger.Patterns.logPhonepeApiCall(log, "DQR_INIT", baseUrl + endpoint,
                wrapper, transactionId);

            WebClient webClient = webClientFactory.createWebClient();
            long phonepeCallStart = System.currentTimeMillis();
            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + endpoint)
                    .header("X-VERIFY", xVerify)
                    .header("X-CALLBACK-URL", callback)
                    .header("Content-Type", "application/json")
                    .header("X-CALL-MODE", "POST")
                    .header("X-PROVIDER-ID", dqrInitializeTransactionRequest.getProvider())
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
                .operation("DQR_INIT_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            StructuredLogger.Patterns.logPhonepeApiResponse(log, "DQR_INIT", baseUrl + endpoint,
                response, System.currentTimeMillis() - phonepeCallStart, 200);

            if (response == null) {
                throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            dqrInitializeTransactionRequestRepository.save(request);
            DqrInitializeTransactionResponse initializeTransactionResponse = objectMapper.readValue(response, DqrInitializeTransactionResponse.class);
            DqrResponseCode responseCode = DqrResponseCode.fromString(initializeTransactionResponse.getCode());

            if (initializeTransactionResponse.getData() != null && initializeTransactionResponse.getData().getAmount() > 0) {
                initializeTransactionResponse.getData().setAmount(initializeTransactionResponse.getData().getAmount() / 100);
            }

            dqrInitializeTransactionResponseRepository.save(initializeTransactionResponse);

            byte[] imageData = generateQrImage(initializeTransactionResponse.getData().getQrString());

            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_INIT_SUCCESS")
                .transactionId(transactionId)
                .phonepeTransactionId(initializeTransactionResponse.getData().getTransactionId())
                .field("qrGenerated", true)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("DQR transaction initialization completed successfully");

            HttpHeaders headers = new HttpHeaders();
            headers.set("transactionId", initializeTransactionResponse.getData().getTransactionId());
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                    .headers(headers)
                    .body(imageData);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_INIT_JSON_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during DQR initialization", e);
            throw new DqrApiException(DqrResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_INIT_WEBCLIENT_ERROR")
                .transactionId(transactionId)
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during DQR initialization", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (DqrApiException e) {
            throw e;
        } catch (Exception e) {
            commonServiceUtils.handleDqrApiError(e, transactionId, "INIT", log);
            return null; // This line will never be reached as handleDqrApiError always throws
        }
    }

    /**
     * Generates a QR code image from the provided UPI payment string.
     *
     * <p>This method creates a QR code image that can be displayed to customers
     * for scanning with UPI-enabled mobile applications. The QR code contains
     * the complete payment information including merchant details, amount, and
     * transaction reference.
     *
     * <p>QR Code Specifications:
     * <ul>
     *   <li><b>Format:</b> PNG image format</li>
     *   <li><b>Size:</b> 300x300 pixels</li>
     *   <li><b>Error Correction:</b> Medium level for better scan reliability</li>
     *   <li><b>Encoding:</b> UTF-8 character encoding</li>
     * </ul>
     *
     * <p>The generated QR code follows UPI payment standards and contains
     * information such as:
     * <ul>
     *   <li>Payment address (VPA)</li>
     *   <li>Payee name</li>
     *   <li>Transaction reference</li>
     *   <li>Amount</li>
     *   <li>Currency code</li>
     * </ul>
     *
     * <p>Performance Considerations:
     * QR code generation is CPU-intensive. For high-volume applications,
     * consider caching generated QR codes or using asynchronous processing.
     *
     * @param qrString the UPI payment string to encode in the QR code.
     *                 Format: {@code upi://pay?pa=merchant@bank&pn=MerchantName&tr=TXN123&am=100.00&cu=INR}
     * @return byte array containing the PNG image data of the generated QR code
     * @see QRCodeWriter
     * @see MatrixToImageWriter
     */
    public byte[] generateQrImage(String qrString) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("QR_IMAGE_GENERATION_START")
                .field("qrStringLength", qrString.length())
                .timestamp()
                .debug("Starting QR image generation");

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrString, BarcodeFormat.QR_CODE, 500, 500);

            try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "JPEG", pngOutputStream);
                byte[] imageData = pngOutputStream.toByteArray();

                StructuredLogger.forLogger(log)
                    .operation("QR_IMAGE_GENERATION_SUCCESS")
                    .field("imageSizeBytes", imageData.length)
                    .responseTime(System.currentTimeMillis() - startTime)
                    .timestamp()
                    .debug("QR image generation completed");

                return imageData;
            }
        } catch (WriterException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("QR_IMAGE_GENERATION_WRITER_ERROR")
                .field("qrStringLength", qrString.length())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("QR code encoding failed", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "QR code generation failed: " + e.getMessage());
        } catch (IOException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("QR_IMAGE_GENERATION_IO_ERROR")
                .field("qrStringLength", qrString.length())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("QR image stream writing failed", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "QR image generation failed: " + e.getMessage());
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("QR_IMAGE_GENERATION_UNEXPECTED_ERROR")
                .field("qrStringLength", qrString.length())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Unexpected error during QR generation", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "Unexpected QR generation error: " + e.getMessage());
        }
    }

    public ResponseEntity<?> cancelTransaction(DqrCancelTransactionRequest dqrCancelTransactionRequest) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_CANCEL_START")
                .transactionId(dqrCancelTransactionRequest.getTransactionId())
                .field("merchantId", dqrCancelTransactionRequest.getMerchantId())
                .field("reason", dqrCancelTransactionRequest.getReason())
                .timestamp()
                .info("Starting DQR transaction cancellation");
            String path = cancelEndPoint + dqrCancelTransactionRequest.getMerchantId() + "/" + dqrCancelTransactionRequest.getTransactionId() + cancelEndPointPath;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);

            WebClient webClient = webClientFactory.createWebClient();
            var response = webClientFactory.withRetry(
                webClient.post().uri(baseUrl + path)
                    .header("X-VERIFY", xVerify)
                    .header("Content-Type", "application/json")
                    .header("X-PROVIDER-ID", dqrCancelTransactionRequest.getProvider())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(dqrCancelTransactionRequest)
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
                .operation("DQR_CANCEL_RESPONSE_RECEIVED")
                .transactionId(dqrCancelTransactionRequest.getTransactionId())
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            if (response == null) {
                throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "Empty response from PhonePe");
            }

            dqrCancelTransactionRequest.setCancelledAt(LocalDateTime.now());
            DqrCancelTransactionResponse dqrInitializeTransactionResponse = objectMapper.readValue(response, DqrCancelTransactionResponse.class);
            dqrCancelTransactionRequestRepository.save(dqrCancelTransactionRequest);
            dqrCancelTransactionResponseRepository.save(dqrInitializeTransactionResponse);

            DqrResponseCode responseCode = DqrResponseCode.fromString(dqrInitializeTransactionResponse.getCode());
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_CANCEL_SUCCESS")
                .transactionId(dqrCancelTransactionRequest.getTransactionId())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("DQR transaction cancellation completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(dqrInitializeTransactionResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_CANCEL_JSON_ERROR")
                .transactionId(dqrCancelTransactionRequest.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during DQR cancellation", e);
            throw new DqrApiException(DqrResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_CANCEL_WEBCLIENT_ERROR")
                .transactionId(dqrCancelTransactionRequest.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during DQR cancellation", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (DqrApiException e) {
            throw e;
        } catch (Exception e) {
            commonServiceUtils.handleDqrApiError(e, dqrCancelTransactionRequest.getTransactionId(), "CANCEL", log);
            return null; // This line will never be reached as handleDqrApiError always throws
        }
    }

    public ResponseEntity<?> refund(DqrRefundTransactionRequest request) {
        long startTime = System.currentTimeMillis();

        try {
           String transactionId = GenerateTransactionId.generateTransactionId(numeric);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_REFUND_START")
                .transactionId(transactionId)
                .amount(request.getAmount())
                .field("merchantOrderId", request.getMerchantOrderId())
                .timestamp()
                .info("Starting DQR transaction refund");
            request.setAmount(request.getAmount()*100);
            String jsonPayload = objectMapper.writeValueAsString(request);
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
                    .header("X-PROVIDER-ID", request.getProvider())
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
                .operation("DQR_REFUND_RESPONSE_RECEIVED")
                .transactionId(transactionId)
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            request.setTransactionId(transactionId);
            DqrRefundTransactionRequest tx = DqrRefundTransactionRequest.builder().transactionId(request.getTransactionId()).merchantOrderId(request.getMerchantOrderId()).amount((long) request.getAmount()).createdAt(LocalDateTime.now()).build();
            dqrRefundTransactionRequestRepository.save(tx);
            DqrRefundTransactionResponse refundTransactionResponse = objectMapper.readValue(response, DqrRefundTransactionResponse.class);

            if (refundTransactionResponse.getAmount() > 0) {
                refundTransactionResponse.setAmount(refundTransactionResponse.getAmount() / 100);
            }

            dqrRefundTransactionResponseRepository.save(refundTransactionResponse);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_REFUND_SUCCESS")
                .transactionId(request.getTransactionId())
                .amount(request.getAmount())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("DQR transaction refund completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(refundTransactionResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_REFUND_JSON_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during DQR refund", e);
            throw new DqrApiException(DqrResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_REFUND_WEBCLIENT_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during DQR refund", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (DqrApiException e) {
            throw e;
        } catch (Exception e) {
            commonServiceUtils.handleDqrApiError(e, request.getTransactionId(), "REFUND", log);
            return null; // This line will never be reached as handleDqrApiError always throws
        }
    }

    public ResponseEntity<?> checkPaymentStatus(DqrCheckTransactionStatusRequest request) throws JsonProcessingException {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_STATUS_CHECK_START")
                .transactionId(request.getTransactionId())
                .field("merchantId", request.getMerchantId())
                .timestamp()
                .info("Starting DQR payment status check");
            String path = checkPaymentStatusEndPoint + request.getMerchantId() + "/" + request.getTransactionId() + checkPaymentStatusEndPointPath;
            String xVerify = GenerateXVerifyKey.generateXVerify(path, saltKey, saltIndex);
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
                .operation("DQR_STATUS_RESPONSE_RECEIVED")
                .transactionId(request.getTransactionId())
                .field("rawResponse", response)
                .field("responseLength", response != null ? response.length() : 0)
                .timestamp()
                .info("Raw PhonePe API response received");

            request.setCheckedStatusAt(LocalDateTime.now());
            dqrCheckTransactionStatusRequestRepository.save(request);
            DqrCheckTransactionStatusResponse checkTransactionStatusResponse = objectMapper.readValue(response, DqrCheckTransactionStatusResponse.class);

            if (checkTransactionStatusResponse.getData() != null && checkTransactionStatusResponse.getData().getAmount() != null) {
                checkTransactionStatusResponse.getData().setAmount(checkTransactionStatusResponse.getData().getAmount() / 100);
            }

            dqrCheckTransactionStatusResponseRepository.save(checkTransactionStatusResponse);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_STATUS_CHECK_SUCCESS")
                .transactionId(request.getTransactionId())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("DQR payment status check completed successfully");

            return ResponseEntity.status(HttpStatus.OK).body(checkTransactionStatusResponse);
        } catch (JsonProcessingException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_STATUS_CHECK_JSON_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("JSON processing failed during DQR status check", e);
            throw new DqrApiException(DqrResponseCode.BAD_REQUEST, "Invalid request format: " + e.getMessage());
        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("DQR_SERVICE_STATUS_CHECK_WEBCLIENT_ERROR")
                .transactionId(request.getTransactionId())
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("PhonePe API call failed during DQR status check", e);
            throw new DqrApiException(DqrResponseCode.INTERNAL_SERVER_ERROR, "PhonePe service unavailable: " + e.getMessage());
        } catch (DqrApiException e) {
            throw e;
        } catch (Exception e) {
            commonServiceUtils.handleDqrApiError(e, request.getTransactionId(), "STATUS_CHECK", log);
            return null; // This line will never be reached as handleDqrApiError always throws
        }
    }


}
