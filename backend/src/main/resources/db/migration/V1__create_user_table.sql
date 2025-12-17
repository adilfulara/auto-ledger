-- Create app schema (if not exists - V4 will also try to create it)
CREATE SCHEMA IF NOT EXISTS app;

-- Create users table in app schema
-- User authentication is managed externally by auth provider
-- Internal UUID provides stable identity independent of auth provider

CREATE TABLE app.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_provider_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common lookups
CREATE INDEX idx_users_email ON app.users(email);
CREATE INDEX idx_users_auth_provider_id ON app.users(auth_provider_id);

-- Add comments for documentation
COMMENT ON TABLE app.users IS 'User accounts with internal identity decoupled from auth provider';
COMMENT ON COLUMN app.users.id IS 'Internal stable UUID (never changes, even if auth provider changes)';
COMMENT ON COLUMN app.users.auth_provider_id IS 'External identifier from auth provider (e.g., Clerk user_2abc123xyz)';
COMMENT ON COLUMN app.users.email IS 'User email address from auth provider';
