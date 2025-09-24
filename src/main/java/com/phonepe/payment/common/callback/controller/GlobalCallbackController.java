package com.phonepe.payment.common.callback.controller;

import com.phonepe.payment.common.callback.entity.CallbackRequest;
import com.phonepe.payment.common.callback.service.GlobalCallbackService;
import com.phonepe.payment.util.CommonServiceUtils;
import com.phonepe.payment.util.FailureApiResponse;
import com.phonepe.payment.util.StructuredLogger;
import com.phonepe.payment.exception.TrackExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Global REST Controller for handling PhonePe S2S (Server-to-Server) callback operations.
 *
 * <p>This controller provides a unified endpoint for processing callback notifications
 * from PhonePe for all payment modules including StaticQR, DQR, EDC, and PaymentLink
 * transactions. It handles X-VERIFY signature validation and routes callbacks to
 * appropriate services.</p>
 *
 * @author Naveen Manyam
 * @version 1.0
 * @see GlobalCallbackService
 * @since 1.0
 */
@RestController
@RequestMapping("/api/phonepe")
@Slf4j
public class GlobalCallbackController {

    private final GlobalCallbackService globalCallbackService;
    private final TrackExceptionService trackExceptionService;
    private final CommonServiceUtils commonServiceUtils;

    @Autowired
    public GlobalCallbackController(GlobalCallbackService globalCallbackService, TrackExceptionService trackExceptionService, CommonServiceUtils commonServiceUtils) {
        this.globalCallbackService = globalCallbackService;
        this.trackExceptionService = trackExceptionService;
        this.commonServiceUtils = commonServiceUtils;
    }

    @PostMapping("/s2s-callback")
    @Operation(summary = "PhonePe S2S Callback Endpoint", description = "Global S2S callback endpoint for all PhonePe payment modules (StaticQR, DQR, EDC, PaymentLink)")
    @ApiResponse(responseCode = "200", description = "Callback processed successfully")
    @ApiResponse(responseCode = "400", description = "Bad request (invalid payload)")
    @ApiResponse(responseCode = "401", description = "Unauthorized (X-VERIFY validation failed)")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> handleS2SCallback(
            @Parameter(description = "Base64 encoded callback response", required = true) @RequestBody CallbackRequest callbackRequest,
            @Parameter(description = "X-VERIFY header for authentication", required = true) @RequestHeader("X-VERIFY") String xVerify) {

        long startTime = System.currentTimeMillis();
        String requestId = StructuredLogger.generateRequestId();
        try {
            StructuredLogger.forLogger(log).operation("PHONEPE_S2S_CALLBACK").requestId(requestId).field("requestBodyLength", callbackRequest.getResponse().length()).field("xVerifyPresent", xVerify != null && !xVerify.isEmpty()).timestamp().info("PhonePe S2S callback received");
            ResponseEntity<FailureApiResponse<Object>> validationResult = commonServiceUtils.validateRequest(
                    callbackRequest,
                    "response"
            );
            if (validationResult != null) {
                return validationResult;
            }
            globalCallbackService.processCallback(callbackRequest, xVerify);
            StructuredLogger.forLogger(log).operation("PHONEPE_S2S_CALLBACK").requestId(requestId).responseTime(System.currentTimeMillis() - startTime).httpStatus(HttpStatus.OK.value()).timestamp().info("PhonePe S2S callback processed successfully");

            return ResponseEntity.ok().body(FailureApiResponse.builder()
                    .success(true)
                    .code("200")
                    .message("Callback processed successfully")
                    .build());
        } catch (SecurityException e) {
            StructuredLogger.forLogger(log).operation("PHONEPE_S2S_CALLBACK").requestId(requestId).error(e).responseTime(System.currentTimeMillis() - startTime).httpStatus(HttpStatus.UNAUTHORIZED.value()).timestamp().warn("S2S callback rejected - X-VERIFY validation failed");
            trackExceptionService.logException(e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid X-VERIFY");

        } catch (IllegalArgumentException e) {
            StructuredLogger.forLogger(log).operation("PHONEPE_S2S_CALLBACK").requestId(requestId).error(e).responseTime(System.currentTimeMillis() - startTime).httpStatus(HttpStatus.BAD_REQUEST.value()).timestamp().warn("S2S callback rejected - invalid payload");
            trackExceptionService.logException(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");

        } catch (Exception e) {
            trackExceptionService.logException(e);
            StructuredLogger.forLogger(log).operation("PHONEPE_S2S_CALLBACK").requestId(requestId).error(e).responseTime(System.currentTimeMillis() - startTime).httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()).timestamp().error("Error processing PhonePe S2S callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Callback processing failed");

        } finally {
            StructuredLogger.clearMDC();
        }
    }
}