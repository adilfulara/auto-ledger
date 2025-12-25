# Clerk Testing Scripts

Helper scripts for testing Clerk authentication with Auto Ledger **staging environment**.

> **Note**: These testing scripts are designed for staging only. They help developers quickly test API endpoints with JWT authentication.

## Prerequisites

1. **Clerk account created** with test user
2. **Get your credentials from Clerk Dashboard:**
   - Go to: Configure → API Keys
   - Copy **Secret Key** (starts with `sk_test_`)
   - Go to: Users → Click on test user
   - Copy **User ID** (starts with `user_`)

## Setup

Set environment variables:

```bash
export CLERK_SECRET_KEY="sk_test_i5VXLfVZZwcheOSiajE0BFb2Ubg1e5Vm8689ggHONM"
export CLERK_USER_ID="user_37EArbYrtgdYxlycoChHB5jNZR2"
```

**Tip:** Add these to your `~/.bashrc` or `~/.zshrc` to make them permanent.

## Usage

### 1. Issue JWT Token

Generates a fresh JWT token from Clerk:

```bash
./scripts/clerk/testing/1-issue-jwt.sh
```

This will:
- Create a session for your test user
- Generate a JWT using the `testing-template`
- Display the JWT claims
- Save the JWT to `/tmp/clerk_jwt.txt`

**Output:**
```
═══════════════════════════════════════════════════════
  Clerk JWT Token Generator
═══════════════════════════════════════════════════════

ℹ Secret Key: sk_test_i5VXLfVZZwch...
ℹ User ID: user_37EArbYrtgdYxlycoChHB5jNZR2

➤ Creating session...
✓ Session created: sess_abc123...

➤ Generating JWT with 'testing-template'...
✓ JWT generated successfully

═══════════════════════════════════════════════════════
  JWT Claims:
═══════════════════════════════════════════════════════
{
  "aud": "auto-ledger-staging",
  "email": "test@example.com",
  "sub": "user_37EArbYrtgdYxlycoChHB5jNZR2",
  ...
}

✓ JWT saved to: /tmp/clerk_jwt.txt
```

### 2. Get All Cars

Tests GET /api/cars endpoint:

```bash
# Test staging
./scripts/clerk/testing/2-get-cars.sh staging
```

This will:
- Test API without JWT (should get 401)
- Test API with JWT (should get 200)
- Display the cars array

### 3. Create a Car

Tests POST /api/cars endpoint:

```bash
# Create car in staging
./scripts/clerk/testing/3-create-car.sh staging
```

This will:
- Create a Tesla Model 3 test car
- Display the created car's ID

### 4. Delete a Car

Tests DELETE /api/cars/{id} endpoint:

```bash
# Delete a car (interactive - shows list)
./scripts/clerk/testing/4-delete-car.sh staging

# Delete specific car by ID
./scripts/clerk/testing/4-delete-car.sh staging abc-123-def
```

This will:
- List all your cars (if no ID provided)
- Delete the specified car
- Verify the deletion

## Common Workflows

### Test Authentication End-to-End

```bash
# 1. Get fresh JWT
./scripts/clerk/testing/1-issue-jwt.sh

# 2. Verify you have no cars
./scripts/clerk/testing/2-get-cars.sh staging

# 3. Create a car
./scripts/clerk/testing/3-create-car.sh staging

# 4. Verify the car was created
./scripts/clerk/testing/2-get-cars.sh staging

# 5. Delete the car
./scripts/clerk/testing/4-delete-car.sh staging

# 6. Verify the car was deleted
./scripts/clerk/testing/2-get-cars.sh staging
```

### Test with Manual JWT Export

```bash
# Get JWT
./scripts/clerk/testing/1-issue-jwt.sh

# Export it
export JWT=$(cat /tmp/clerk_jwt.txt)

# Use in other scripts
./scripts/clerk/testing/2-get-cars.sh staging
```

## Troubleshooting

### "CLERK_SECRET_KEY not set"

Set the environment variable:
```bash
export CLERK_SECRET_KEY="sk_test_..."
```

### "JWT might be expired"

JWTs are valid for 10 minutes. Generate a fresh one:
```bash
./scripts/clerk/testing/1-issue-jwt.sh
```

### "Invalid JWT signature"

This can happen if:
- The JWT template wasn't configured correctly
- The issuer URI in Fly secrets doesn't match Clerk

Fix:
1. Check Fly secrets: `fly secrets list -a auto-ledger-staging`
2. Verify `JWT_ISSUER_URI` matches your Clerk domain
3. Generate a fresh JWT: `./scripts/clerk/testing/1-issue-jwt.sh`

### "Authentication failed (HTTP 401)"

Check backend logs:
```bash
fly logs -a auto-ledger-staging | grep -i "jwt\|auth"
```

Common issues:
- JWT expired (generate fresh token)
- Audience mismatch (check JWT template has correct `aud` claim)
- Issuer mismatch (check `JWT_ISSUER_URI` in Fly secrets)

## Files

| Script | Purpose |
|--------|---------|
| `1-issue-jwt.sh` | Generate fresh JWT token from Clerk |
| `2-get-cars.sh` | Test GET /api/cars endpoint |
| `3-create-car.sh` | Test POST /api/cars endpoint |
| `4-delete-car.sh` | Test DELETE /api/cars/{id} endpoint |
| `README.md` | This file |

## Related Documentation

- **Clerk Setup Guide:** `../../../docs/auth/CLERK-SETUP.md`
- **Staging Auth:** `../../../docs/auth/STAGING-AUTH.md`
- **Main Scripts README:** `../README.md`
