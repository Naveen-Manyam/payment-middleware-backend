package com.phonepe.payment.dqr.controller;

import com.phonepe.payment.dqr.entity.*;
import com.phonepe.payment.dqr.service.DqrService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DqrController Tests")
class DqrControllerTest {

    @Mock
    private DqrService dqrService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private DqrController dqrController;

    @Nested
    @DisplayName("Initialize DQR Transaction Tests")
    class InitTransactionTests {

        private DqrInitializeTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new DqrInitializeTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setAmount(10000L);
            validRequest.setStoreId("STORE123");
            validRequest.setTerminalId("TERM123");
            validRequest.setProvider("phonepe");
            validRequest.setTransactionId("TXN123456");
        }

        @Test
        @DisplayName("Should generate QR successfully with valid request")
        void shouldGenerateQrSuccessfully() {
            byte[] qrImageBytes = new byte[]{1, 2, 3, 4, 5};
            ResponseEntity<?> expectedResponse = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImageBytes);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = dqrController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
            assertThat(actualResponse.getBody()).isEqualTo(qrImageBytes);
            verify(dqrService).initTransaction(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("amount"), eq("storeId"), eq("terminalId"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing merchantId")
        void shouldReturn400ForMissingMerchantId() {
            DqrInitializeTransactionRequest invalidRequest = new DqrInitializeTransactionRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("merchantId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = dqrController.initTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(dqrService, never()).initTransaction(any());
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
            doReturn(expectedResponse).when(dqrService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = dqrController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should handle exception during QR generation")
        void shouldHandleExceptionDuringQrGeneration() {
            RuntimeException exception = new RuntimeException("QR generation failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(dqrService).initTransaction(validRequest);

            assertThatThrownBy(() -> dqrController.initTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("QR generation failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Cancel Payment Tests")
    class CancelPaymentTests {

        private DqrCancelTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new DqrCancelTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setReason("Customer requested cancellation");
            validRequest.setProvider("phonepe");
        }

        @Test
        @DisplayName("Should cancel payment successfully")
        void shouldCancelPaymentSuccessfully() {
            DqrCancelTransactionResponse response = new DqrCancelTransactionResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = dqrController.cancelPayment(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(dqrService).cancelTransaction(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("transactionId"), eq("reason"), eq("provider"));
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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = dqrController.cancelPayment(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should handle exception during cancellation")
        void shouldHandleExceptionDuringCancellation() {
            RuntimeException exception = new RuntimeException("Cancellation failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(dqrService).cancelTransaction(validRequest);

            assertThatThrownBy(() -> dqrController.cancelPayment(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cancellation failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Refund Payment Tests")
    class RefundTests {

        private DqrRefundTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new DqrRefundTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setOriginalTransactionId("TXN123456");
            validRequest.setAmount(10000L);
            validRequest.setMerchantOrderId("ORDER123");
            validRequest.setMessage("Refund for order cancellation");
            validRequest.setProvider("phonepe");
            validRequest.setTransactionId("REFUND123");
        }

        @Test
        @DisplayName("Should process refund successfully")
        void shouldProcessRefundSuccessfully() {
            DqrRefundTransactionResponse response = new DqrRefundTransactionResponse();
            response.setStatus("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).refund(validRequest);

            ResponseEntity<?> actualResponse = dqrController.refund(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(dqrService).refund(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("originalTransactionId"), eq("amount"),
                eq("merchantOrderId"), eq("message"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for invalid refund request")
        void shouldReturn400ForInvalidRefundRequest() {
            DqrRefundTransactionRequest invalidRequest = new DqrRefundTransactionRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("Invalid refund request")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = dqrController.refund(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(dqrService, never()).refund(any());
        }

        @Test
        @DisplayName("Should return 404 for original transaction not found")
        void shouldReturn404ForOriginalTransactionNotFound() {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("NOT_FOUND")
                .message("Original transaction not found")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).refund(validRequest);

            ResponseEntity<?> actualResponse = dqrController.refund(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should handle exception during refund")
        void shouldHandleExceptionDuringRefund() {
            RuntimeException exception = new RuntimeException("Refund failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(dqrService).refund(validRequest);

            assertThatThrownBy(() -> dqrController.refund(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refund failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Check Payment Status Tests")
    class GetPaymentStatusTests {

        private DqrCheckTransactionStatusRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new DqrCheckTransactionStatusRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
        }

        @Test
        @DisplayName("Should retrieve transaction status successfully")
        void shouldRetrieveTransactionStatusSuccessfully() throws Exception {
            DqrCheckTransactionStatusResponse response = new DqrCheckTransactionStatusResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(dqrService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = dqrController.getPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(dqrService).checkPaymentStatus(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("transactionId"), eq("provider"));
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
            doReturn(expectedResponse).when(dqrService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = dqrController.getPaymentStatus(validRequest);

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
            doReturn(expectedResponse).when(dqrService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = dqrController.getPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during status check")
        void shouldHandleExceptionDuringStatusCheck() throws Exception {
            Exception exception = new Exception("Status check failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(dqrService).checkPaymentStatus(validRequest);

            assertThatThrownBy(() -> dqrController.getPaymentStatus(validRequest))
                .isInstanceOf(Exception.class)
                .hasMessage("Status check failed");
        }
    }
}