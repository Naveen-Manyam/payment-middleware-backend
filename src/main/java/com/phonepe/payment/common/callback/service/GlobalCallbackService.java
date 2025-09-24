package com.phonepe.payment.common.callback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phonepe.payment.common.callback.entity.CallbackRequest;
import com.phonepe.payment.common.callback.entity.CallbackResponse;
import com.phonepe.payment.common.callback.repository.CallbackRequestRepository;
import com.phonepe.payment.common.callback.repository.CallbackResponseRepository;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.exception.TrackExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Global service for handling PhonePe S2S (Server-to-Server) callback operations.
 *
 * <p>This service provides centralized callback processing for all PhonePe payment modules
 * including StaticQR, DQR, EDC, and PaymentLink transactions. It handles X-VERIFY signature
 * validation and processes callback notifications from PhonePe's payment gateway.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @since 1.0
 * @see CallbackRequest
 * @see CallbackResponse
 */
@Service
@Slf4j
public class GlobalCallbackService {

    @Autowired
    private CallbackRequestRepository callbackRequestRepository;

    @Autowired
    private CallbackResponseRepository callbackResponseRepository;

    @Autowired
    private TrackExceptionService trackExceptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${static.phonepe.saltKey}")
    private String saltKey;

    @Value("${static.phonepe.saltIndex}")
    private String saltIndex;

    public boolean validateXVerify(CallbackRequest callBackRequest, String xVerifyHeader) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("X_VERIFY_VALIDATION_START")
                .field("responseBodyLength", callBackRequest.getResponse().length())
                .field("xVerifyPresent", xVerifyHeader != null && !xVerifyHeader.isEmpty())
                .timestamp()
                .debug("Starting X-VERIFY validation");

            String toHash = callBackRequest.getResponse() + saltKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String expectedXVerify = hexString + "###" + saltIndex;
            boolean isValid = expectedXVerify.equals(xVerifyHeader);

            StructuredLogger.forLogger(log)
                .operation("X_VERIFY_VALIDATION_RESULT")
                .field("isValid", isValid)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .debug("X-VERIFY validation completed");

            return isValid;
        } catch (NoSuchAlgorithmException e) {
            StructuredLogger.forLogger(log)
                .operation("X_VERIFY_VALIDATION_ALGORITHM_ERROR")
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("SHA-256 algorithm not available for X-VERIFY validation", e);
            return false;
        } catch (Exception e) {
            StructuredLogger.forLogger(log)
                .operation("X_VERIFY_VALIDATION_UNEXPECTED_ERROR")
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Unexpected error during X-VERIFY validation", e);
            return false;
        }
    }

    public void processCallback(CallbackRequest callBackRequest, String xVerifyHeader) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredLogger.forLogger(log)
                .operation("PHONEPE_CALLBACK_PROCESSING_START")
                .field("responseBodyLength", callBackRequest.getResponse().length())
                .timestamp()
                .info("Starting PhonePe callback processing");

            boolean xVerifyValid = validateXVerify(callBackRequest, xVerifyHeader);

            callBackRequest.setXVerifyHeader(xVerifyHeader);
            callBackRequest.setXVerifyValid(xVerifyValid);
            callBackRequest.setCreatedAt(LocalDateTime.now());

            if (!xVerifyValid) {
                callBackRequest.setErrorMessage("X-VERIFY validation failed");
                callbackRequestRepository.save(callBackRequest);
                throw new SecurityException("Invalid X-VERIFY header");
            }
            callbackRequestRepository.save(callBackRequest);

            String decodedResponse = new String(Base64.getDecoder().decode(callBackRequest.getResponse()), StandardCharsets.UTF_8);

            JsonNode responseJson = objectMapper.readTree(decodedResponse);
            CallbackResponse callbackResponse = extractCallbackResponse(responseJson, decodedResponse);
            callbackResponse.setCreatedAt(LocalDateTime.now());

            callbackResponseRepository.save(callbackResponse);

            StructuredLogger.forLogger(log)
                .operation("PHONEPE_CALLBACK_PROCESSING_SUCCESS")
                .field("transactionId", callbackResponse.getTransactionId())
                .field("merchantId", callbackResponse.getMerchantId())
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .info("PhonePe callback processing completed successfully");

        } catch (SecurityException e) {
            StructuredLogger.forLogger(log)
                .operation("PHONEPE_CALLBACK_PROCESSING_SECURITY_ERROR")
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Security validation failed during PhonePe callback processing", e);
            throw e;
        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log)
                .operation("PHONEPE_CALLBACK_PROCESSING_UNEXPECTED_ERROR")
                .error(e)
                .responseTime(System.currentTimeMillis() - startTime)
                .timestamp()
                .error("Unexpected error during PhonePe callback processing", e);
            throw new RuntimeException("Callback processing failed: " + e.getMessage(), e);
        }
    }

    private CallbackResponse extractCallbackResponse(JsonNode responseJson, String rawJsonData) throws JsonProcessingException {
        CallbackResponse.CallbackResponseBuilder builder = CallbackResponse.builder()
            .rawJsonData(rawJsonData)
            .createdAt(LocalDateTime.now());

        if (responseJson.has("success")) {
            builder.success(responseJson.get("success").asBoolean());
        }
        if (responseJson.has("code")) {
            builder.code(responseJson.get("code").asText());
        }
        if (responseJson.has("message")) {
            builder.message(responseJson.get("message").asText());
        }

        JsonNode dataNode = responseJson.get("data");
        if (dataNode != null) {
            if (dataNode.has("transactionId")) {
                builder.transactionId(dataNode.get("transactionId").asText());
            }
            if (dataNode.has("merchantId")) {
                builder.merchantId(dataNode.get("merchantId").asText());
            }
            if (dataNode.has("providerReferenceId")) {
                builder.providerReferenceId(dataNode.get("providerReferenceId").asText());
            }
            if (dataNode.has("amount")) {
                builder.amount(dataNode.get("amount").asInt());
            }
            if (dataNode.has("paymentState")) {
                builder.paymentState(dataNode.get("paymentState").asText());
            }
            if (dataNode.has("payResponseCode")) {
                builder.payResponseCode(dataNode.get("payResponseCode").asText());
            }

            JsonNode transactionContextNode = dataNode.get("transactionContext");
            if (transactionContextNode != null) {
                Map<String, String> transactionContext = new HashMap<>();
                transactionContextNode.fields().forEachRemaining(entry ->
                    transactionContext.put(entry.getKey(), entry.getValue().asText()));
                builder.transactionContext(transactionContext);
            }

            JsonNode paymentModesNode = dataNode.get("paymentModes");
            if (paymentModesNode != null && paymentModesNode.isArray()) {
                Map<String, String> paymentModes = new HashMap<>();
                for (int i = 0; i < paymentModesNode.size(); i++) {
                    JsonNode mode = paymentModesNode.get(i);
                    String modeType = mode.has("mode") ? mode.get("mode").asText() : "mode_" + i;
                    paymentModes.put(modeType, mode.toString());
                }
                builder.paymentModes(paymentModes);
            }
        }

        return builder.build();
    }

}