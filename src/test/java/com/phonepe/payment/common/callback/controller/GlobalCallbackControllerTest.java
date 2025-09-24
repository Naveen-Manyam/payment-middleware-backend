package com.phonepe.payment.common.callback.controller;

import com.phonepe.payment.common.callback.entity.CallbackRequest;
import com.phonepe.payment.common.callback.service.GlobalCallbackService;
import com.phonepe.payment.exception.TrackExceptionService;
import com.phonepe.payment.util.CommonServiceUtils;
import com.phonepe.payment.util.FailureApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalCallbackController Tests")
class GlobalCallbackControllerTest {

    @Mock
    private GlobalCallbackService globalCallbackService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private GlobalCallbackController globalCallbackController;

    @Nested
    @DisplayName("S2S Callback Tests")
    class S2SCallbackTests {

        private CallbackRequest validRequest;
        private String validXVerify;

        @BeforeEach
        void setUp() {
            validRequest = new CallbackRequest();
            validRequest.setResponse("base64EncodedCallbackResponse");
            validXVerify = "valid-x-verify-signature";
        }

        @Test
        @DisplayName("Should process callback successfully with valid X-VERIFY")
        void shouldProcessCallbackSuccessfullyWithValidXVerify() {
            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doNothing().when(globalCallbackService).processCallback(validRequest, validXVerify);

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            FailureApiResponse<?> responseBody = (FailureApiResponse<?>) response.getBody();
            assertThat(responseBody.isSuccess()).isTrue();
            assertThat(responseBody.getCode()).isEqualTo("200");
            assertThat(responseBody.getMessage()).isEqualTo("Callback processed successfully");
            verify(globalCallbackService).processCallback(validRequest, validXVerify);
            verify(commonServiceUtils).validateRequest(eq(validRequest), eq("response"));
        }

        @Test
        @DisplayName("Should return 400 for missing response field")
        void shouldReturn400ForMissingResponseField() {
            CallbackRequest invalidRequest = new CallbackRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("response is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            when(commonServiceUtils.validateRequest(eq(invalidRequest), any())).thenReturn(expectedResponse);

            ResponseEntity<?> actualResponse = globalCallbackController.handleS2SCallback(invalidRequest, validXVerify);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(globalCallbackService, never()).processCallback(any(), any());
        }

        @Test
        @DisplayName("Should return 401 for invalid X-VERIFY validation")
        void shouldReturn401ForInvalidXVerifyValidation() {
            SecurityException exception = new SecurityException("Invalid X-VERIFY signature");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(validRequest, validXVerify);

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isEqualTo("Invalid X-VERIFY");
            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should return 400 for invalid payload (IllegalArgumentException)")
        void shouldReturn400ForInvalidPayload() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid payload format");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(validRequest, validXVerify);

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isEqualTo("Invalid payload");
            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should return 500 for internal server error")
        void shouldReturn500ForInternalServerError() {
            RuntimeException exception = new RuntimeException("Internal server error");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(validRequest, validXVerify);

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isEqualTo("Callback processing failed");
            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should log security exception for X-VERIFY failure")
        void shouldLogSecurityExceptionForXVerifyFailure() {
            SecurityException exception = new SecurityException("X-VERIFY validation failed");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(validRequest, validXVerify);

            globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should track exceptions using TrackExceptionService")
        void shouldTrackExceptionsUsingTrackExceptionService() {
            Exception exception = new Exception("Generic exception");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(validRequest, validXVerify);

            globalCallbackController.handleS2SCallback(validRequest, validXVerify);

            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should process callback with null X-VERIFY header")
        void shouldProcessCallbackWithNullXVerifyHeader() {
            SecurityException exception = new SecurityException("X-VERIFY header is required");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(eq(validRequest), eq(null));

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(trackExceptionService).logException(exception);
        }

        @Test
        @DisplayName("Should process callback with empty X-VERIFY header")
        void shouldProcessCallbackWithEmptyXVerifyHeader() {
            String emptyXVerify = "";
            SecurityException exception = new SecurityException("X-VERIFY header is empty");

            when(commonServiceUtils.validateRequest(eq(validRequest), any())).thenReturn(null);
            doThrow(exception).when(globalCallbackService).processCallback(eq(validRequest), eq(emptyXVerify));

            ResponseEntity<?> response = globalCallbackController.handleS2SCallback(validRequest, emptyXVerify);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(trackExceptionService).logException(exception);
        }
    }
}