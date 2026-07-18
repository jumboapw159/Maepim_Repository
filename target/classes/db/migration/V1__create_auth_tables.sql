-- =====================================================
-- MAEPIM DATABASE
-- PostgreSQL 15+
-- =====================================================

-- Create Schema
CREATE SCHEMA IF NOT EXISTS maepim;

SET search_path TO maepim;

-- =====================================================
-- USER STATUS
-- =====================================================

CREATE TYPE user_status AS ENUM
(
    'ACTIVE',
    'INACTIVE',
    'LOCKED',
    'DELETED'
);

-- =====================================================
-- ROLES
-- =====================================================

CREATE TABLE roles
(
    id              BIGSERIAL PRIMARY KEY,
    role_name       VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- USERS
-- =====================================================

CREATE TABLE users
(
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(100) NOT NULL UNIQUE,
    email               VARCHAR(255) UNIQUE,
    phone               VARCHAR(20) UNIQUE,
    password_hash       TEXT NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    profile_image       TEXT,
--     status              user_status NOT NULL DEFAULT 'ACTIVE',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    last_login          TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- USER ROLES
-- =====================================================

CREATE TABLE user_roles
(
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY(role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

-- =====================================================
-- USER ADDRESS
-- =====================================================

CREATE TABLE addresses
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    address_name        VARCHAR(100),
    receiver_name       VARCHAR(255),
    phone               VARCHAR(20),
    address             TEXT NOT NULL,
    subdistrict         VARCHAR(100),
    district            VARCHAR(100),
    province            VARCHAR(100),
    postal_code         VARCHAR(10),
    is_default          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =====================================================
-- REFRESH TOKENS
-- =====================================================

CREATE TABLE refresh_tokens
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    token           TEXT NOT NULL UNIQUE,
    expiry_date     TIMESTAMP NOT NULL,
    revoked         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- =====================================================
-- LOGIN HISTORY
-- =====================================================

CREATE TABLE login_history
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    ip_address      VARCHAR(100),
    user_agent      TEXT,
    login_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success         BOOLEAN,
    CONSTRAINT fk_login_history_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_users_username
ON users(username);

CREATE INDEX idx_users_email
ON users(email);

CREATE INDEX idx_users_phone
ON users(phone);

CREATE INDEX idx_addresses_user
ON addresses(user_id);

CREATE INDEX idx_refresh_user
ON refresh_tokens(user_id);

CREATE INDEX idx_login_history_user
ON login_history(user_id);

-- =====================================================
-- AUTO-UPDATE TIMESTAMP FUNCTION AND TRIGGER
-- =====================================================

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_timestamp
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE update_timestamp();

-- =====================================================
-- DEFAULT ROLES
-- =====================================================

INSERT INTO roles(role_name, description)
VALUES
('ROLE_SUPER_ADMIN', 'System Owner'),
('ROLE_ADMIN', 'Administrator'),
('ROLE_SALES', 'Sales Staff'),
('ROLE_GRAPHIC', 'Graphic Designer'),
('ROLE_FACTORY', 'Factory Staff'),
('ROLE_QC', 'Quality Control'),
('ROLE_ACCOUNTING', 'Accounting'),
('ROLE_CUSTOMER', 'Customer');