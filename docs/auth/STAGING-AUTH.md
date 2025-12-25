# Staging Authentication Setup

> **📖 For detailed Clerk setup instructions**, see [CLERK-SETUP.md](CLERK-SETUP.md)

## Current State (No Auth Provider)

Authentication is **disabled** in staging by default. This guide shows how to enable it with Clerk.

### How It Works

When `auth.jwt.enabled: false` (default in staging):
- All API requests succeed without requiring JWT
- The system automatically uses a test user (Alice from sample data)
- Alice's UUID: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`

### Testing the APIs

You can test all CRUD endpoints immediately:

```bash
# List Alice's cars (returns Tesla Model 3 and Honda Civic)
curl https://auto-ledger-staging.fly.dev/api/cars

# Create a new car for Alice
curl -X POST https://auto-ledger-staging.fly.dev/api/cars \
  -H "Content-Type: application/json" \
  -d '{
    "make": "Toyota",
    "model": "Camry",
    "year": 2023,
    "distanceUnit": "MILES",
    "volumeUnit": "GALLONS"
  }'
```

No authentication headers required!

---

## Enabling Authentication with Clerk

### Quick Start

```bash
# Using the automated script (recommended)
make auth-enable-staging

# Or manually
./scripts/setup-clerk-auth.sh staging
```

### Detailed Setup

For a comprehensive step-by-step guide to setting up Clerk, see:

**📖 [`CLERK-SETUP.md`](CLERK-SETUP.md)**

The guide covers:
- Creating Clerk account and application
- Configuring application settings
- Getting issuer URI and audience values
- Creating test users
- Getting JWT tokens
- Troubleshooting common issues

### Testing Authentication

```bash
# Get a JWT token from Clerk (see CLERK-SETUP.md)
export JWT="your-jwt-token-here"

# Test using the script
make auth-test-staging ARGS="--jwt $JWT"

# Or manually
curl -H "Authorization: Bearer $JWT" \
  https://auto-ledger-staging.fly.dev/api/cars
```

On first request, the system will:
1. Validate the JWT against Clerk's JWKS endpoint
2. Create a user record in the database (JIT provisioning)
3. Return that user's cars (may be empty for new users)

---

## Disabling Authentication

To disable auth and return to test user mode:

```bash
# Using the Makefile
make auth-disable-staging

# Or manually
fly secrets set AUTH_ENABLED=false -a auto-ledger-staging
```

This will redeploy the app and revert to using Alice's test user.

---

## Switching Auth Providers

The backend uses provider-agnostic JWT validation (no Clerk SDK). Switching providers only requires updating environment variables:

```bash
# Switch from Clerk to Auth0
fly secrets set JWT_ISSUER_URI=https://your-tenant.auth0.com/ -a auto-ledger-staging

# Switch from Auth0 to Supabase
fly secrets set JWT_ISSUER_URI=https://your-project.supabase.co/auth/v1 -a auto-ledger-staging
```

No code changes needed! The backend validates JWTs using standard JWKS.

---

## For Local Development

Auth is also disabled in local development (application-local.yml):

```bash
# Start with sample data
make dev-start

# APIs use Alice's test user automatically
curl http://localhost:9090/api/cars
```
