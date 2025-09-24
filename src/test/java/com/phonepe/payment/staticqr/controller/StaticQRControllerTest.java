package com.phonepe.payment.staticqr.controller;

import com.phonepe.payment.staticqr.entity.*;
import com.phonepe.payment.staticqr.service.StaticQRService;
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
@DisplayName("StaticQRController Tests")
class StaticQRControllerTest {

    @Mock
    private StaticQRService staticQRService;

    @Mock
    private TrackExceptionService trackExceptionService;

    @Mock
    private CommonServiceUtils commonServiceUtils;

    @InjectMocks
    private StaticQRController staticQRController;

    @Nested
    @DisplayName("Transaction List Tests")
    class TransactionListTests {

        private StaticQRTransactionListRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new StaticQRTransactionListRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setProvider("phonepe");
            validRequest.setSize(10);
            validRequest.setStoreId("STORE123");
        }

        @Test
        @DisplayName("Should return transaction list successfully for valid request")
        void shouldReturnTransactionListSuccessfullyForValidRequest() {
            StaticQRTransactionListResponse response = new StaticQRTransactionListResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionList(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(staticQRService).getTransactionList(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("provider"), eq("size"), eq("storeId"));
        }

        @Test
        @DisplayName("Should return 400 for missing merchantId")
        void shouldReturn400ForMissingMerchantId() {
            StaticQRTransactionListRequest invalidRequest = new StaticQRTransactionListRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("merchantId is required")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequiredFields(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = staticQRController.initTransaction(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(staticQRService, never()).getTransactionList(any());
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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionList(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 404 when transaction does not exist")
        void shouldReturn404WhenTransactionDoesNotExist() {
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("NOT_FOUND")
                .message("Transaction not found")
                .build();
            ResponseEntity<?> expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionList(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.initTransaction(validRequest);

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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionList(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.initTransaction(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during transaction list retrieval")
        void shouldHandleExceptionDuringTransactionListRetrieval() {
            RuntimeException exception = new RuntimeException("Transaction list retrieval failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(staticQRService).getTransactionList(validRequest);

            assertThatThrownBy(() -> staticQRController.initTransaction(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transaction list retrieval failed");

            verify(trackExceptionService).logException(exception);
        }
    }

    @Nested
    @DisplayName("Transaction Metadata Tests")
    class TransactionMetadataTests {

        private StaticQRMetadataRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new StaticQRMetadataRequest();
            validRequest.setMerchantId("M123456");
            validRequest.setProvider("phonepe");
            validRequest.setPhonepeTransactionId("PPETXN123456");
            validRequest.setSchemaVersionNumber("1.0");
        }

        @Test
        @DisplayName("Should return transaction metadata successfully for valid request")
        void shouldReturnTransactionMetadataSuccessfullyForValidRequest() {
            StaticQRMetadataResponse response = new StaticQRMetadataResponse();
            response.setSuccess(true);
            response.setCode("SUCCESS");
            ResponseEntity<?> expectedResponse = ResponseEntity.ok(response);

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionMetadata(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.getTransactionMetadata(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(actualResponse.getBody()).isEqualTo(response);
            verify(staticQRService).getTransactionMetadata(validRequest);
            verify(commonServiceUtils).validateRequiredFields(eq(validRequest),
                eq("merchantId"), eq("provider"), eq("phonepeTransactionId"), eq("schemaVersionNumber"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() {
            StaticQRMetadataRequest invalidRequest = new StaticQRMetadataRequest();
            FailureApiResponse<Object> errorResponse = FailureApiResponse.builder()
                .success(false)
                .code("BAD_REQUEST")
                .message("Required fields missing")
                .build();
            ResponseEntity<FailureApiResponse<Object>> expectedResponse = ResponseEntity.badRequest().body(errorResponse);

            doReturn(expectedResponse).when(commonServiceUtils).validateRequiredFields(eq(invalidRequest), any());

            ResponseEntity<?> actualResponse = staticQRController.getTransactionMetadata(invalidRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(staticQRService, never()).getTransactionMetadata(any());
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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionMetadata(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.getTransactionMetadata(validRequest);

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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionMetadata(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.getTransactionMetadata(validRequest);

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

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doReturn(expectedResponse).when(staticQRService).getTransactionMetadata(validRequest);

            ResponseEntity<?> actualResponse = staticQRController.getTransactionMetadata(validRequest);

            assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle exception during metadata retrieval")
        void shouldHandleExceptionDuringMetadataRetrieval() {
            RuntimeException exception = new RuntimeException("Metadata retrieval failed");

            doReturn(null).when(commonServiceUtils).validateRequiredFields(eq(validRequest), any());
            doThrow(exception).when(staticQRService).getTransactionMetadata(validRequest);

            assertThatThrownBy(() -> staticQRController.getTransactionMetadata(validRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Metadata retrieval failed");

            verify(trackExceptionService).logException(exception);
        }
    }
}