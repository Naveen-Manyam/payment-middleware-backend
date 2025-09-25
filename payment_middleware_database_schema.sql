-- =============================================================================
-- UNIFIED PAYMENT MIDDLEWARE BACKEND - COMPLETE DATABASE SCHEMA
-- =============================================================================
-- Database: PostgreSQL
-- Tables
-- Indexes
-- =============================================================================

-- =============================================================================
-- 1. COLLECT CALL TABLES
-- =============================================================================

CREATE TABLE collect_call_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    merchant_order_id VARCHAR(255) NOT NULL,
    amount INTEGER NOT NULL,
    instrument_type VARCHAR(255) NOT NULL,
    instrument_reference VARCHAR(255) NOT NULL,
    message TEXT,
    email VARCHAR(255),
    expires_in INTEGER,
    short_name VARCHAR(255),
    store_id VARCHAR(255),
    terminal_id VARCHAR(255),
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_cancel_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_cancel_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_check_transaction_status_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_check_transaction_status_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    payment_state VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_refund_transactions_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    original_transaction_id VARCHAR(255) NOT NULL,
    merchant_order_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collect_call_refund_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 2. PAYMENT LINK TABLES
-- =============================================================================

CREATE TABLE payment_link_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255),
    transaction_id VARCHAR(255),
    merchant_order_id VARCHAR(255),
    amount INTEGER,
    mobile_number VARCHAR(255),
    message TEXT,
    expires_in INTEGER,
    store_id VARCHAR(255),
    terminal_id VARCHAR(255),
    provider VARCHAR(255),
    short_name VARCHAR(255),
    sub_merchant_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    upi_intent TEXT,
    pay_link TEXT,
    mobile_number VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_cancel_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_cancel_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_check_transaction_status_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_check_transaction_status_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    payment_state VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_refund_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    original_transaction_id VARCHAR(255) NOT NULL,
    merchant_order_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_link_refund_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. DYNAMIC QR (DQR) TABLES
-- =============================================================================

CREATE TABLE dqr_initialize_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255),
    transaction_id VARCHAR(255),
    merchant_order_id VARCHAR(255),
    amount BIGINT,
    expires_in INTEGER,
    store_id VARCHAR(255),
    terminal_id VARCHAR(255),
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_initialize_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    -- Embedded DataDto fields
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    qr_string VARCHAR(5000), -- Base64 encoded QR string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_cancel_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_cancel_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_check_transaction_status_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_check_transaction_status_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    payment_state VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_refund_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    original_transaction_id VARCHAR(255) NOT NULL,
    merchant_order_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dqr_refund_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. EDC (ELECTRONIC DATA CAPTURE) TABLES
-- =============================================================================

CREATE TABLE edc_initialize_transaction_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255),
    store_id VARCHAR(255),
    order_id VARCHAR(255),
    transaction_id VARCHAR(255),
    merchant_order_id VARCHAR(255),
    terminal_id VARCHAR(255),
    integration_mapping_type VARCHAR(255),
    payment_modes TEXT,
    amount BIGINT,
    provider VARCHAR(255),
    time_allowed_for_handover_to_terminal_seconds INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edc_initialize_transaction_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    merchant_id VARCHAR(255),
    transaction_id VARCHAR(255),
    amount INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edc_check_transaction_status_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edc_check_transaction_status_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    transaction_id VARCHAR(255),
    amount INTEGER,
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    payment_state VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 5. STATIC QR TABLES
-- =============================================================================

CREATE TABLE static_qr_metadata_request (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255),
    provider VARCHAR(255),
    phonepe_transaction_id VARCHAR(255),
    schema_version_number VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collection Table: static_qr_metadata_request_metadata
CREATE TABLE static_qr_metadata_request_metadata (
    request_id BIGINT NOT NULL,
    metadata_key VARCHAR(255) NOT NULL,
    metadata_value TEXT,
    PRIMARY KEY (request_id, metadata_key),
    FOREIGN KEY (request_id) REFERENCES static_qr_metadata_request(id) ON DELETE CASCADE
);

CREATE TABLE static_qr_metadata_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE static_qr_transaction_list_request (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(255),
    merchant_id VARCHAR(255),
    store_id VARCHAR(255),
    size INTEGER,
    amount BIGINT,
    start_timestamp BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE static_qr_transaction_list_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE static_qr_callback_request (
    id BIGSERIAL PRIMARY KEY,
    response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE static_qr_callback_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 6. COMMON CALLBACK TABLES
-- =============================================================================

CREATE TABLE phonepe_callback_request (
    id BIGSERIAL PRIMARY KEY,
    response TEXT, -- Base64 encoded response from PhonePe
    x_verify_header VARCHAR(255),
    x_verify_valid BOOLEAN,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE phonepe_callback_response (
    id BIGSERIAL PRIMARY KEY,
    success BOOLEAN,
    code VARCHAR(255),
    message VARCHAR(255),
    transaction_id VARCHAR(255),
    merchant_id VARCHAR(255),
    provider_reference_id VARCHAR(255),
    amount INTEGER,
    payment_state VARCHAR(255),
    pay_response_code VARCHAR(255),
    mobile_number VARCHAR(255),
    phone_number VARCHAR(255),
    customer_name VARCHAR(255),
    raw_json_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Collection Table: phonepe_callback_transaction_context
CREATE TABLE phonepe_callback_transaction_context (
    response_id BIGINT NOT NULL,
    context_key VARCHAR(255) NOT NULL,
    context_value TEXT,
    PRIMARY KEY (response_id, context_key),
    FOREIGN KEY (response_id) REFERENCES phonepe_callback_response(id) ON DELETE CASCADE
);

-- Collection Table: phonepe_callback_payment_modes
CREATE TABLE phonepe_callback_payment_modes (
    response_id BIGINT NOT NULL,
    mode_type VARCHAR(255) NOT NULL,
    mode_details TEXT,
    PRIMARY KEY (response_id, mode_type),
    FOREIGN KEY (response_id) REFERENCES phonepe_callback_response(id) ON DELETE CASCADE
);

-- =============================================================================
-- 7. EXCEPTION TRACKING TABLE
-- =============================================================================

-- Table: exception_track
CREATE TABLE exception_track (
    id BIGSERIAL PRIMARY KEY,
    message TEXT,
    exception TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- =============================================================================

CREATE INDEX idx_collect_call_request_transaction_id ON collect_call_request(transaction_id);
CREATE INDEX idx_collect_call_request_merchant_id ON collect_call_request(merchant_id);
CREATE INDEX idx_collect_call_request_merchant_order_id ON collect_call_request(merchant_order_id);

CREATE INDEX idx_payment_link_request_transaction_id ON payment_link_request(transaction_id);
CREATE INDEX idx_payment_link_request_merchant_id ON payment_link_request(merchant_id);
CREATE INDEX idx_payment_link_request_merchant_order_id ON payment_link_request(merchant_order_id);

CREATE INDEX idx_dqr_initialize_request_transaction_id ON dqr_initialize_transaction_request(transaction_id);
CREATE INDEX idx_dqr_initialize_request_merchant_id ON dqr_initialize_transaction_request(merchant_id);

CREATE INDEX idx_edc_initialize_request_transaction_id ON edc_initialize_transaction_request(transaction_id);
CREATE INDEX idx_edc_initialize_request_merchant_id ON edc_initialize_transaction_request(merchant_id);

CREATE INDEX idx_static_qr_metadata_request_merchant_id ON static_qr_metadata_request(merchant_id);
CREATE INDEX idx_static_qr_transaction_list_request_merchant_id ON static_qr_transaction_list_request(merchant_id);

CREATE INDEX idx_phonepe_callback_response_transaction_id ON phonepe_callback_response(transaction_id);
CREATE INDEX idx_phonepe_callback_response_merchant_id ON phonepe_callback_response(merchant_id);

CREATE INDEX idx_collect_call_request_created_at ON collect_call_request(created_at);
CREATE INDEX idx_payment_link_request_created_at ON payment_link_request(created_at);
CREATE INDEX idx_dqr_initialize_request_created_at ON dqr_initialize_transaction_request(created_at);
CREATE INDEX idx_edc_initialize_request_created_at ON edc_initialize_transaction_request(created_at);
CREATE INDEX idx_static_qr_metadata_request_created_at ON static_qr_metadata_request(created_at);
CREATE INDEX idx_phonepe_callback_request_created_at ON phonepe_callback_request(created_at);
CREATE INDEX idx_phonepe_callback_response_created_at ON phonepe_callback_response(created_at);
CREATE INDEX idx_exception_track_created_at ON exception_track(created_at);

CREATE INDEX idx_collect_call_request_provider ON collect_call_request(provider);
CREATE INDEX idx_payment_link_request_provider ON payment_link_request(provider);
CREATE INDEX idx_dqr_initialize_request_provider ON dqr_initialize_transaction_request(provider);
CREATE INDEX idx_edc_initialize_request_provider ON edc_initialize_transaction_request(provider);

CREATE INDEX idx_collect_call_request_store_terminal ON collect_call_request(store_id, terminal_id);
CREATE INDEX idx_payment_link_request_store_terminal ON payment_link_request(store_id, terminal_id);
CREATE INDEX idx_dqr_initialize_request_store_terminal ON dqr_initialize_transaction_request(store_id, terminal_id);
CREATE INDEX idx_edc_initialize_request_terminal ON edc_initialize_transaction_request(terminal_id);

-- =============================================================================