-- Create users table
-- User authentication is managed externally by Clerk
-- Internal UUID provides stable identity independent of auth provider

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_provider_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common lookups
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_auth_provider_id ON users(auth_provider_id);

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts with internal identity decoupled from auth provider';
COMMENT ON COLUMN users.id IS 'Internal stable UUID (never changes, even if auth provider changes)';
COMMENT ON COLUMN users.auth_provider_id IS 'External identifier from auth provider (e.g., Clerk user_2abc123xyz)';
COMMENT ON COLUMN users.email IS 'User email address from auth provider';
