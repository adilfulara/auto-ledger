# Clerk Authentication Scripts

Scripts for managing Clerk authentication in Auto Ledger staging/production environments.

## Overview

| Script | Purpose |
|--------|---------|
| `setup.sh` | Enable Clerk authentication (set Fly.io secrets) |
| `test.sh` | Validate authentication is working (3 test cases) |
| `testing/` | Developer helper scripts for API testing |

## Quick Start

### Enable Authentication in Staging

```bash
# Interactive mode (prompts for Clerk issuer URI and audience)
./scripts/clerk/setup.sh staging

# Or use the Makefile
make auth-enable-staging
```

###  Test Authentication

```bash
# Requires a valid JWT token from Clerk
./scripts/clerk/test.sh staging --jwt "$JWT"

# Or use the Makefile
make auth-test-staging ARGS="--jwt $JWT"
```

### Testing API Endpoints

For interactive API testing, see [testing/README.md](testing/README.md).

```bash
# 1. Generate JWT
./scripts/clerk/testing/1-issue-jwt.sh

# 2. Test API endpoints
./scripts/clerk/testing/2-get-cars.sh staging
./scripts/clerk/testing/3-create-car.sh staging
```

## Prerequisites

- **Fly CLI**: `brew install flyctl && fly auth login`
- **Clerk account**: See [docs/auth/CLERK-SETUP.md](../../docs/auth/CLERK-SETUP.md)
- **Fly.io app**: Must exist before setting auth secrets

## Related Documentation

- **Complete Setup Guide**: [docs/auth/CLERK-SETUP.md](../../docs/auth/CLERK-SETUP.md)
- **Quick Reference**: [STAGING-AUTH.md](../../docs/auth/STAGING-AUTH.md)
- **Testing Guide**: [testing/README.md](testing/README.md)
- **Issue #54**: Backend Clerk authentication setup
