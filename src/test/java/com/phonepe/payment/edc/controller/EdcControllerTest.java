package com.phonepe.payment.edc.controller;

import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusRequest;
import com.phonepe.payment.edc.entity.EdcCheckTransactionStatusResponse;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionRequest;
import com.phonepe.payment.edc.entity.EdcInitializeTransactionResponse;
import com.phonepe.payment.edc.service.EdcService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EdcController Tests")
class EdcControllerTest {

    @Mock
    private EdcService edcService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private EdcController edcController;

    @Nested
    @DisplayName("Initialize EDC Transaction Tests")
    class InitTransactionTests {

        private EdcInitializeTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new EdcInitializeTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setStoreId("STORE123");
            validRequest.setOrderId("ORDER123");
            validRequest.setTerminalId("TERM123");
            validRequest.setAmount(10000L);
            validRequest.setProvider("phonepe");
            validRequest.setTransactionId("TXN123456");
        }

        @Test
        @DisplayName("Should initialize EDC transaction successfully with valid request")
        void shouldInitializeEdcTransactionSuccessfully() {
            EdcInitializeTransactionResponse response = new EdcInitializeTransactionResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = edcController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(edcService).initTransaction(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("storeId"), eq("orderId"), eq("terminalId"),
                eq("amount"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing merchantId")
        void shouldReturn400ForMissingMerchantId() {
            EdcInitializeTransactionRequest invalidRequest = new EdcInitializeTransactionRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("merchantId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = edcController.initTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(edcService, never()).initTransaction(any());
        }

        @Test
        @DisplayName("Should return 400 for missing storeId")
        void shouldReturn400ForMissingStoreId() {
            EdcInitializeTransactionRequest invalidRequest = new EdcInitializeTransactionRequest();
            invalidRequest.setMerchantId("M123456");
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("storeId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = edcController.initTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(edcService, never()).initTransaction(any());
        }

        @Test
        @DisplayName("Should return 401 for X-VERIFY mismatch")
        void shouldReturn401ForXVerifyMismatch() {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("UNAUTHORIZED")
                .message("X-VERIFY mismatch")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = edcController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 404 for transaction not found")
        void shouldReturn404ForTransactionNotFound() {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("NOT_FOUND")
                .message("Transaction not found")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = edcController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 500 for PhonePe server error")
        void shouldReturn500ForPhonePeServerError() {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("INTERNAL_SERVER_ERROR")
                .message("PhonePe server error")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = edcController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during EDC initialization")
        void shouldHandleExceptionDuringEdcInitialization() {
            RuntimeException exception = new RuntimeException("EDC initialization failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(edcService).initTransaction(validRequest);

            assertThatThrownBy(() -> edcController.initTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("EDC initialization failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Check Payment Status Tests")
    class GetPaymentStatusTests {

        private EdcCheckTransactionStatusRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new EdcCheckTransactionStatusRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
        }

        @Test
        @DisplayName("Should retrieve payment status successfully")
        void shouldRetrievePaymentStatusSuccessfully() throws Exception {
            EdcCheckTransactionStatusResponse response = new EdcCheckTransactionStatusResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = edcController.getPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(edcService).checkPaymentStatus(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("transactionId"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            EdcCheckTransactionStatusRequest invalidRequest = new EdcCheckTransactionStatusRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("Required fields missing")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequiredFields(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = edcController.getPaymentStatus(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(edcService, never()).checkPaymentStatus(any());
        }

        @Test
        @DisplayName("Should return 404 for transaction not found")
        void shouldReturn404ForTransactionNotFound() throws Exception {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("NOT_FOUND")
                .message("Transaction not found")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = edcController.getPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should return 500 for internal server error")
        void shouldReturn500ForInternalServerError() throws Exception {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("INTERNAL_SERVER_ERROR")
                .message("Internal server error")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(edcService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = edcController.getPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during status check")
        void shouldHandleExceptionDuringStatusCheck() throws Exception {
            Exception exception = new Exception("Status check failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(edcService).checkPaymentStatus(validRequest);

            assertThatThrownBy(() -> edcController.getPaymentStatus(validRequest))
                .isInstanceOf(Exception.class)
                .hasMessage("Status check failed");

            verify(trackExceptionService).logException(exception);
        }
    }
}