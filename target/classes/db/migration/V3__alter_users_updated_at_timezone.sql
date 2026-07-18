-- =====================================================
-- MAEPIM DATABASE
-- PostgreSQL 15+
-- =====================================================

-- Create Schema
CREATE SCHEMA IF NOT EXISTS maepim;

SET search_path TO maepim;

-- Set the session timezone for this migration
SET TIME ZONE 'Asia/Bangkok';

-- Alter the created_at column in the users table to TIMESTAMP WITH TIME ZONE
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC';

-- Ensure the default value for created_at is also TIMESTAMP WITH TIME ZONE
ALTER TABLE users
ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

-- Alter the updated_at column in the users table to TIMESTAMP WITH TIME ZONE
ALTER TABLE users
ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';

-- Ensure the default value for updated_at is also TIMESTAMP WITH TIME ZONE
ALTER TABLE users
ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;