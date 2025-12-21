# Staging Authentication Setup

## Current State (No Auth Provider)

Authentication is **disabled** in staging until you configure an auth provider (Clerk, Auth0, etc.).

### How It Works

When `auth.jwt.enabled: false` (default in staging):
- All API requests succeed without requiring JWT
- The system automatically uses a test user (Alice from sample data)
- Alice's UUID: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`

### Testing the APIs

You can test all CRUD endpoints immediately:

```bash
# List Alice's cars (returns Tesla Model 3 and Honda Civic)
curl https://your-app.fly.dev/api/cars

# Create a new car for Alice
curl -X POST https://your-app.fly.dev/api/cars \
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

## Enabling Authentication (When Ready)

When you choose and configure an auth provider:

### 1. Set up your auth provider
- Create account with Clerk/Auth0/Supabase Auth
- Configure application and get issuer URI

### 2. Configure Fly.io secrets

```bash
fly secrets set AUTH_ENABLED=true
fly secrets set JWT_ISSUER_URI=https://your-clerk-domain.com
fly secrets set JWT_AUDIENCE=auto-ledger-staging
```

### 3. Deploy

```bash
git push origin main  # Or your deployment branch
```

### 4. Test with JWT

```bash
# Get JWT from your auth provider's login flow
export JWT="your-jwt-token-here"

# List cars with authentication
curl https://your-app.fly.dev/api/cars \
  -H "Authorization: Bearer $JWT"
```

On first request, the system will:
1. Validate the JWT
2. Create a user record in the database (JIT provisioning)
3. Return that user's cars

---

## Switching Auth Providers

Because the backend is provider-agnostic, switching providers only requires updating environment variables:

```bash
# Switch from Clerk to Auth0
fly secrets set JWT_ISSUER_URI=https://your-tenant.auth0.com/

# Switch from Auth0 to Supabase
fly secrets set JWT_ISSUER_URI=https://your-project.supabase.co/auth/v1
```

No code changes needed!

---

## For Local Development

Auth is also disabled in local development (application-local.yml):

```bash
# Start with sample data
make dev-start

# APIs use Alice's test user automatically
curl http://localhost:9090/api/cars
```
