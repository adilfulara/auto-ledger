# Clerk Authentication Setup Guide

This guide walks you through setting up Clerk as the authentication provider for Auto Ledger (staging and production).

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Create Clerk Account](#create-clerk-account)
3. [Create Clerk Application](#create-clerk-application)
4. [Configure Application Settings](#configure-application-settings)
5. [Get Authentication Credentials](#get-authentication-credentials)
6. [Configure Backend Environment](#configure-backend-environment)
7. [Create Test User](#create-test-user)
8. [Get Test JWT Token](#get-test-jwt-token)
9. [Test Authentication](#test-authentication)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Fly.io CLI installed and authenticated (`fly auth login`)
- Access to your Auto Ledger Fly.io app(s)
- Email address for Clerk account

---

## Create Clerk Account

1. Go to [https://clerk.com](https://clerk.com)
2. Click "Start Building for Free"
3. Sign up with your email or GitHub account
4. Verify your email address

---

## Create Clerk Application

1. From the Clerk Dashboard, click **"Create Application"**
2. Configure your application:
   - **Name**: `Auto Ledger Staging` (or `Auto Ledger Production`)
   - **Sign-in options**: Select **Email** (minimum)
     - Optional: Add social providers (Google, GitHub, etc.)
   - Click **"Create Application"**

3. You'll be redirected to the application dashboard

---

## Configure Application Settings

### Allow Staging/Production Origins

1. In Clerk Dashboard, go to **Configure** → **Allowed origins**
2. Add your backend URL:
   - **Staging**: `https://auto-ledger-staging.fly.dev`
   - **Production**: `https://auto-ledger-prod.fly.dev`
3. Click **"Add origin"**

### Session Token Configuration

Clerk's default JWT configuration works out-of-the-box with Auto Ledger. The backend expects:

- **`sub` claim**: User ID from Clerk (stored as `auth_provider_id`)
- **`email` claim**: User email address

These are included by default - no custom template needed!

---

## Get Authentication Credentials

You need three values to configure the backend:

### 1. Issuer URI

The Issuer URI is your Clerk application's unique identifier.

1. In Clerk Dashboard, go to **Configure** → **API Keys**
2. Find **"Frontend API"** value (e.g., `clerk.auto-ledger.1a2b3c.lcl.dev`)
3. Your **Issuer URI** is:
   ```
   https://<frontend-api-value>
   ```

   **Example**:
   ```
   https://clerk.auto-ledger.1a2b3c.lcl.dev
   ```

### 2. Audience (Optional)

Clerk doesn't require a specific audience by default. You can:

**Option A**: Use the application name
```
auto-ledger-staging
```

**Option B**: Leave as default
```
auto-ledger
```

### 3. Summary

You should now have:

| Variable | Example Value |
|----------|---------------|
| `AUTH_ENABLED` | `true` |
| `JWT_ISSUER_URI` | `https://clerk.auto-ledger.1a2b3c.lcl.dev` |
| `JWT_AUDIENCE` | `auto-ledger-staging` |

---

## Configure Backend Environment

### Using the Setup Script (Recommended)

```bash
# Preview what will be set
./scripts/setup-clerk-auth.sh staging --dry-run

# Run interactively (prompts for values)
./scripts/setup-clerk-auth.sh staging

# Or provide all values upfront
./scripts/setup-clerk-auth.sh staging \
  --issuer-uri "https://clerk.auto-ledger.1a2b3c.lcl.dev" \
  --audience "auto-ledger-staging"
```

### Manual Configuration

If you prefer to set secrets manually:

```bash
# For staging
fly secrets set -a auto-ledger-staging \
  AUTH_ENABLED=true \
  JWT_ISSUER_URI="https://clerk.auto-ledger.1a2b3c.lcl.dev" \
  JWT_AUDIENCE="auto-ledger-staging"

# For production
fly secrets set -a auto-ledger-prod \
  AUTH_ENABLED=true \
  JWT_ISSUER_URI="https://clerk.auto-ledger.1a2b3c.lcl.dev" \
  JWT_AUDIENCE="auto-ledger-prod"
```

**Note**: Setting secrets will trigger a deployment.

---

## Create Test User

1. In Clerk Dashboard, go to **Users**
2. Click **"Create User"**
3. Fill in:
   - **Email**: Your test email (e.g., `test@example.com`)
   - **Password**: Set a password
4. Click **"Create"**

Your test user is now ready!

---

## Get Test JWT Token

You need a JWT token to test the backend authentication.

### Method 1: Clerk Dashboard (Easiest)

1. In Clerk Dashboard, go to **Users**
2. Click on your test user
3. Go to the **"Sessions"** tab
4. Find the active session (or create one by signing in via your frontend)
5. Click **"Copy JWT"** or use the API to get the token

**Note**: Dashboard JWTs expire quickly (typically 1 hour). You may need to regenerate.

### Method 2: Sign In via Frontend (Future)

Once you have a frontend:
1. Sign in with your test user
2. Open browser DevTools → Console
3. Run: `await window.Clerk.session.getToken()`
4. Copy the JWT

### Method 3: Clerk Backend API

```bash
# Get session token via Clerk API
curl -X POST "https://api.clerk.com/v1/sessions/<session_id>/tokens" \
  -H "Authorization: Bearer <clerk_secret_key>" \
  -H "Content-Type: application/json"
```

---

## Test Authentication

### Using the Test Script (Recommended)

```bash
# Interactive mode (prompts for JWT)
./scripts/test-clerk-auth.sh staging

# Or provide JWT directly
export JWT="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
./scripts/test-clerk-auth.sh staging --jwt "$JWT"
```

The script will test:
- ✅ Health endpoint (should be 200 without auth)
- ✅ API endpoint without JWT (should be 401)
- ✅ API endpoint with JWT (should be 200)

### Manual Testing

```bash
# 1. Health check (no auth required)
curl https://auto-ledger-staging.fly.dev/actuator/health
# Expected: {"status":"UP"}

# 2. Cars API without JWT (should fail)
curl https://auto-ledger-staging.fly.dev/api/cars
# Expected: 401 Unauthorized

# 3. Cars API with JWT (should succeed)
export JWT="your-jwt-token-here"
curl -H "Authorization: Bearer $JWT" \
  https://auto-ledger-staging.fly.dev/api/cars
# Expected: 200 OK with JSON array (may be empty for new users)
```

---

## Troubleshooting

### Issue: "Invalid issuer" error

**Cause**: `JWT_ISSUER_URI` doesn't match the JWT's `iss` claim.

**Solution**:
1. Decode your JWT at [jwt.io](https://jwt.io)
2. Check the `iss` claim value
3. Ensure `JWT_ISSUER_URI` matches exactly (including `https://`)

### Issue: "Invalid audience" error

**Cause**: JWT's `aud` claim doesn't contain `JWT_AUDIENCE` value.

**Solution**:
1. Check JWT's `aud` claim at [jwt.io](https://jwt.io)
2. Update `JWT_AUDIENCE` to match one of the values in the array
3. Or update Clerk's JWT template to include your desired audience

### Issue: "Token expired" error

**Cause**: JWT has passed its expiration time (`exp` claim).

**Solution**:
1. Get a fresh JWT token from Clerk
2. JWTs from Clerk Dashboard expire in ~1 hour
3. For long-lived testing, consider using the Backend API to refresh tokens

### Issue: "No matching key found in JWKS" error

**Cause**: The JWT's `kid` (key ID) doesn't exist in Clerk's JWKS endpoint.

**Solution**:
1. Verify you're using the correct `JWT_ISSUER_URI`
2. Check `https://{issuer}/.well-known/jwks.json` returns valid keys
3. Ensure your JWT is recent (Clerk may rotate keys)

### Issue: Health check returns 401

**Cause**: Health endpoints should bypass authentication but aren't.

**Solution**:
1. Check backend logs for errors in `JwtAuthFilter`
2. Verify the path is `/actuator/health` (exact match)
3. This is a backend bug if health checks require auth

### Issue: 500 Internal Server Error on first request

**Cause**: JIT user provisioning failed (database issue).

**Solution**:
1. Check backend logs for database connection errors
2. Verify `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` are set
3. Ensure the `app.users` table exists (Flyway migration V1)

---

## Architecture Reference

For developers: The backend uses **provider-agnostic JWT validation**:

1. **No Clerk SDK** - Uses standard JWKS/JWT validation (Nimbus JOSE+JWT library)
2. **JWKS Endpoint** - Fetches public keys from `{issuer}/.well-known/jwks.json`
3. **RSA Signature Verification** - Validates JWT signature with public key
4. **Claims Validation** - Checks `iss`, `aud`, `exp`, `sub`, `email`
5. **JIT User Provisioning** - Creates user on first API request

Key files:
- `backend/src/main/java/me/adilfulara/autoledger/auth/JwtService.java`
- `backend/src/main/java/me/adilfulara/autoledger/auth/JwtAuthFilter.java`
- `backend/src/main/java/me/adilfulara/autoledger/auth/JitUserService.java`

---

## Next Steps

After setting up Clerk authentication:

1. ✅ Test with `./scripts/test-clerk-auth.sh staging`
2. ✅ Verify JIT user provisioning creates database records
3. ✅ Integrate Clerk SDK in frontend (separate issue)
4. ✅ Repeat this process for production environment

For frontend integration, see Clerk's Next.js documentation:
https://clerk.com/docs/quickstarts/nextjs
