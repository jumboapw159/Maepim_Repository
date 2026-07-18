-- =====================================================
-- MAEPIM DATABASE
-- PostgreSQL 15+
-- =====================================================

-- Create Schema
CREATE SCHEMA IF NOT EXISTS maepim;
--
SET search_path TO maepim;

-- =====================================================
-- OTP CODES
-- =====================================================

CREATE TABLE otp_codes
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    code            VARCHAR(6) NOT NULL,
    expiry_date     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_code_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_otp_user
ON otp_codes(user_id);