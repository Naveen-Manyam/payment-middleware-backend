package com.phonepe.payment.paymentlink.controller;

import com.phonepe.payment.exception.TrackExceptionService;
import com.phonepe.payment.paymentlink.entity.*;
import com.phonepe.payment.paymentlink.service.PaymentLinkService;
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
@DisplayName("PaymentLinkController Tests")
class PaymentLinkControllerTest {

    @Mock
    private PaymentLinkService paymentLinkService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private PaymentLinkController paymentLinkController;

    @Nested
    @DisplayName("Initialize Payment Link Tests")
    class InitTransactionTests {

        private PaymentLinkRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new PaymentLinkRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setAmount(Integer.valueOf(10000));
            validRequest.setMobileNumber("9876543210");
            validRequest.setMessage("Payment for order");
            validRequest.setExpiresIn(3600);
            validRequest.setStoreId("STORE123");
            validRequest.setTerminalId("TERM123");
            validRequest.setProvider("phonepe");
            validRequest.setTransactionId("TXN123456");
        }

        @Test
        @DisplayName("Should generate payment link successfully with valid request")
        void shouldGeneratePaymentLinkSuccessfully() {
            PaymentLinkResponse response = new PaymentLinkResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            response.setData(new PaymentLinkResponse.PaymentResponseData());
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(paymentLinkService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(paymentLinkService).initTransaction(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("amount"), eq("mobileNumber"), eq("message"),
                eq("expiresIn"), eq("storeId"), eq("terminalId"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing merchantId")
        void shouldReturn400ForMissingMerchantId() {
            PaymentLinkRequest invalidRequest = new PaymentLinkRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("merchantId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = paymentLinkController.initTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(paymentLinkService, never()).initTransaction(any());
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
            doReturn(expectedResponse).when(paymentLinkService).initTransaction(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should handle exception during payment link generation")
        void shouldHandleExceptionDuringPaymentLinkGeneration() {
            RuntimeException exception = new RuntimeException("Payment link generation failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(paymentLinkService).initTransaction(validRequest);

            assertThatThrownBy(() -> paymentLinkController.initTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Payment link generation failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Cancel Payment Link Transaction Tests")
    class CancelTransactionTests {

        private PaymentLinkCancelTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new PaymentLinkCancelTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
            validRequest.setReason("Customer requested cancellation");
        }

        @Test
        @DisplayName("Should cancel payment link transaction successfully")
        void shouldCancelPaymentLinkTransactionSuccessfully() {
            PaymentLinkCancelTransactionResponse response = new PaymentLinkCancelTransactionResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(paymentLinkService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.cancelTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(paymentLinkService).cancelTransaction(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("transactionId"), eq("provider"), eq("reason"));
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
            doReturn(expectedResponse).when(paymentLinkService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.cancelTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should handle exception during cancellation")
        void shouldHandleExceptionDuringCancellation() {
            RuntimeException exception = new RuntimeException("Cancellation failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(paymentLinkService).cancelTransaction(validRequest);

            assertThatThrownBy(() -> paymentLinkController.cancelTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cancellation failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Check Payment Link Status Tests")
    class CheckPaymentStatusTests {

        private PaymentLinkCheckTransactionStatusRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new PaymentLinkCheckTransactionStatusRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
        }

        @Test
        @DisplayName("Should retrieve status successfully")
        void shouldRetrieveStatusSuccessfully() {
            PaymentLinkCheckTransactionStatusResponse response = new PaymentLinkCheckTransactionStatusResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(paymentLinkService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.checkPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(paymentLinkService).checkPaymentStatus(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("transactionId"), eq("provider"));
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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(paymentLinkService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.checkPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during status check")
        void shouldHandleExceptionDuringStatusCheck() {
            RuntimeException exception = new RuntimeException("Status check failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(paymentLinkService).checkPaymentStatus(validRequest);

            assertThatThrownBy(() -> paymentLinkController.checkPaymentStatus(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Status check failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Refund Payment Link Transaction Tests")
    class RefundTransactionTests {

        private PaymentLinkRefundTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new PaymentLinkRefundTransactionRequest();
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
            PaymentLinkRefundTransactionResponse response = new PaymentLinkRefundTransactionResponse();
            response.setStatus("SUCCESS");
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(paymentLinkService).refundTransaction(validRequest);

            ResponseEntity<?> actualResponse = paymentLinkController.refundTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(paymentLinkService).refundTransaction(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("originalTransactionId"), eq("amount"),
                eq("merchantOrderId"), eq("message"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() {
            PaymentLinkRefundTransactionRequest invalidRequest = new PaymentLinkRefundTransactionRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("Required fields missing")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = paymentLinkController.refundTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(paymentLinkService, never()).refundTransaction(any());
        }

        @Test
        @DisplayName("Should handle exception during refund")
        void shouldHandleExceptionDuringRefund() {
            RuntimeException exception = new RuntimeException("Refund processing failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(paymentLinkService).refundTransaction(validRequest);

            assertThatThrownBy(() -> paymentLinkController.refundTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refund processing failed");

            verify(trackExceptionService).logException(exception);
        }
    }
}