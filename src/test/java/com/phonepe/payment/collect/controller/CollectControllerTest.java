package com.phonepe.payment.collect.controller;

import com.phonepe.payment.collect.entity.*;
import com.phonepe.payment.collect.service.CollectService;
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
@DisplayName("CollectController Tests")
class CollectControllerTest {

    @Mock
    private CollectService collectService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private CollectController collectController;

    @Nested
    @DisplayName("Initiate Collect Call Tests")
    class InitiateCollectCallTests {

        private CollectCallRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CollectCallRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setAmount(Integer.valueOf(10000));
            validRequest.setInstrumentType("UPI");
            validRequest.setInstrumentReference("9876543210@upi");
            validRequest.setProvider("phonepe");
            validRequest.setExpiresIn(Integer.valueOf(300));
            validRequest.setStoreId("STORE123");
            validRequest.setTransactionId("TXN123456");
        }

        @Test
        @DisplayName("Should initiate collect call successfully with valid request")
        void shouldInitiateCollectCallSuccessfully() {
            CollectCallResponse response = new CollectCallResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(collectService).initiateCollectCall(validRequest);

            ResponseEntity<?> actualResponse = collectController.initiateCollectCall(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(collectService).initiateCollectCall(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("amount"), eq("instrumentType"), eq("instrumentReference"),
                eq("provider"), eq("expiresIn"), eq("storeId"));
        }

        @Test
        @DisplayName("Should return 400 for missing merchantId")
        void shouldReturn400ForMissingMerchantId() {
            CollectCallRequest invalidRequest = new CollectCallRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("merchantId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = collectController.initiateCollectCall(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(collectService, never()).initiateCollectCall(any());
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
            doReturn(expectedResponse).when(collectService).initiateCollectCall(validRequest);

            ResponseEntity<?> actualResponse = collectController.initiateCollectCall(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should handle exception and rethrow")
        void shouldHandleExceptionAndRethrow() {
            RuntimeException exception = new RuntimeException("Service error");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(collectService).initiateCollectCall(validRequest);

            assertThatThrownBy(() -> collectController.initiateCollectCall(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service error");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Cancel Transaction Tests")
    class CancelTransactionTests {

        private CollectCallCancelTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CollectCallCancelTransactionRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
            validRequest.setReason("Customer requested cancellation");
        }

        @Test
        @DisplayName("Should cancel transaction successfully")
        void shouldCancelTransactionSuccessfully() {
            CollectCallCancelTransactionResponse response = new CollectCallCancelTransactionResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(collectService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = collectController.cancelTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(collectService).cancelTransaction(validRequest);
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
            doReturn(expectedResponse).when(collectService).cancelTransaction(validRequest);

            ResponseEntity<?> actualResponse = collectController.cancelTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should handle exception during cancellation")
        void shouldHandleExceptionDuringCancellation() {
            RuntimeException exception = new RuntimeException("Cancellation failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(collectService).cancelTransaction(validRequest);

            assertThatThrownBy(() -> collectController.cancelTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cancellation failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Check Payment Status Tests")
    class CheckPaymentStatusTests {

        private CollectCallCheckTransactionStatusRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CollectCallCheckTransactionStatusRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setTransactionId("TXN123456");
            validRequest.setProvider("phonepe");
        }

        @Test
        @DisplayName("Should retrieve status successfully")
        void shouldRetrieveStatusSuccessfully() {
            CollectCallCheckTransactionStatusResponse response = new CollectCallCheckTransactionStatusResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(collectService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = collectController.checkPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(collectService).checkPaymentStatus(validRequest);
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
            doReturn(expectedResponse).when(collectService).checkPaymentStatus(validRequest);

            ResponseEntity<?> actualResponse = collectController.checkPaymentStatus(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Refund Transaction Tests")
    class RefundTransactionTests {

        private CollectCallRefundTransactionRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new CollectCallRefundTransactionRequest();
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
            CollectCallRefundTransactionResponse response = new CollectCallRefundTransactionResponse();
            response.setStatus("SUCCESS");
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doReturn(expectedResponse).when(collectService).refundTransaction(validRequest);

            ResponseEntity<?> actualResponse = collectController.refundTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(collectService).refundTransaction(validRequest);
            verify(commonServiceUtils).validateRequest(eq(validRequest),
                eq("merchantId"), eq("originalTransactionId"), eq("amount"),
                eq("merchantOrderId"), eq("message"), eq("provider"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() {
            CollectCallRefundTransactionRequest invalidRequest = new CollectCallRefundTransactionRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("Required fields missing")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequest(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = collectController.refundTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(collectService, never()).refundTransaction(any());
        }

        @Test
        @DisplayName("Should handle exception during refund")
        void shouldHandleExceptionDuringRefund() {
            RuntimeException exception = new RuntimeException("Refund processing failed");

            doReturn(null).when(commonServiceUtils).validateRequest(eq(validRequest), any());
            doThrow(exception).when(collectService).refundTransaction(validRequest);

            assertThatThrownBy(() -> collectController.refundTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Refund processing failed");

            verify(trackExceptionService).logException(exception);
        }
    }
}