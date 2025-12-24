# Auto Ledger - Infrastructure Scripts

Automation scripts for setting up and managing Auto Ledger infrastructure components.

## Directory Structure

```
scripts/
├── clerk/           # Clerk authentication management
│   ├── setup.sh     # Enable auth in staging
│   ├── test.sh      # Validate auth is working
│   └── testing/     # Developer API testing helpers
├── flyio/           # Fly.io application management
│   ├── setup.sh     # Create Fly.io app
│   └── cleanup.sh   # Delete Fly.io app
└── supabase/        # Supabase database management
    ├── setup.sh     # Create Supabase project
    └── cleanup.sh   # Delete Supabase project
```

## Quick Start

### Initial Infrastructure Setup

```bash
# 1. Create Supabase database
./scripts/supabase/setup.sh staging

# 2. Create Fly.io application
./scripts/flyio/setup.sh staging

# 3. Enable Clerk authentication
./scripts/clerk/setup.sh staging
```

###  Testing & Validation

```bash
# Test authentication is working
./scripts/clerk/test.sh staging --jwt "$JWT"

# Or use Makefile shortcuts
make auth-test-staging ARGS="--jwt $JWT"
```

## Component Guides

Each component has its own detailed README:

- **[clerk/README.md](clerk/README.md)** - Clerk authentication setup and testing
- **[flyio/README.md](flyio/README.md)** - Fly.io application management
- **[supabase/README.md](supabase/README.md)** - Supabase database setup

## Common Workflows

### Fresh Staging Environment Setup

```bash
# Step 1: Database
./scripts/supabase/setup.sh staging

# Step 2: Application
./scripts/flyio/setup.sh staging

# Step 3: Authentication
./scripts/clerk/setup.sh staging

# Step 4: Verify
./scripts/clerk/test.sh staging --jwt "$JWT"
```

### Reset Staging Environment

```bash
# Clean up everything
./scripts/supabase/cleanup.sh staging
./scripts/flyio/cleanup.sh staging

# Recreate from scratch
./scripts/supabase/setup.sh staging
./scripts/flyio/setup.sh staging
./scripts/clerk/setup.sh staging
```

### Test API with Clerk Authentication

```bash
# Generate fresh JWT
export CLERK_SECRET_KEY="sk_test_..."
export CLERK_USER_ID="user_..."
./scripts/clerk/testing/1-issue-jwt.sh

# Test endpoints
./scripts/clerk/testing/2-get-cars.sh staging
./scripts/clerk/testing/3-create-car.sh staging
```

## Makefile Integration

Common operations are available via Makefile:

```bash
# Authentication management
make auth-enable-staging          # Enable Clerk auth
make auth-disable-staging         # Disable auth (test user)
make auth-test-staging ARGS="--jwt $JWT"  # Test auth

# Development
make dev-start                    # Start DB + Backend
make dev-stop                     # Stop everything
make check-coverage               # Verify 80% coverage
```

See [Makefile](../Makefile) for all available commands.

## Script Conventions

All scripts follow these patterns:

### Arguments

```bash
script.sh <environment> [options]

# Environment: staging (production not used with these scripts)
# Options: --dry-run, --force
```

### Dry-Run Mode

Preview changes without executing them:

```bash
./scripts/clerk/setup.sh staging --dry-run
./scripts/flyio/setup.sh staging --dry-run
./scripts/supabase/setup.sh staging --dry-run
```

### Safety Features

| Feature | Description |
|---------|-------------|
| **Idempotent** | Safe to run multiple times |
| **Dry-run mode** | Preview changes with `--dry-run` |
| **Confirmation prompts** | Explicit confirmation for destructive operations |
| **Prerequisite validation** | Checks for required tools before starting |
| **Colored output** | Easy to spot errors (red), warnings (yellow), success (green) |

## Prerequisites

### Required Tools

```bash
# Supabase CLI
brew install supabase/tap/supabase
supabase login

# Fly CLI
brew install flyctl
fly auth login

# GitHub CLI
brew install gh
gh auth login

# jq (JSON processor)
brew install jq

# curl (usually pre-installed)
command -v curl
```

### Required Accounts

- **Supabase**: https://supabase.com (free tier available)
- **Fly.io**: https://fly.io (free tier available)
- **Clerk**: https://clerk.com (free tier available)
- **GitHub**: Repository access for secrets management

## Deployment Architecture

### Registry: Fly.io Container Registry

The project uses **Fly's container registry** (`registry.fly.io`) for Docker images:

**Why Fly Registry?**
- No extra authentication needed
- Simpler workflow than GitHub Packages
- Standard pattern recommended by Fly.io
- Included in Fly.io pricing

**How It Works:**
1. GitHub Actions builds Docker image from `./backend`
2. Logs into `registry.fly.io` using `FLY_API_TOKEN`
3. Pushes image with semantic tag (`main-<sha>` or `pr-<number>-<sha>`)
4. Deploys using `flyctl deploy --image registry.fly.io/auto-ledger-staging:<tag>`

**Required GitHub Secret:**

```bash
# Get your Fly.io token
flyctl auth token

# Store as GitHub secret
gh secret set FLY_API_TOKEN --body "your-fly-token"
```

### Deployment Triggers

| Trigger | Image Tag | Use Case |
|---------|-----------|----------|
| Push to feature branch (with open PR) | `pr-<number>-<sha>` | Test PR before merge |
| Push to `main` | `main-<sha>` | Post-merge verification |
| Manual dispatch | User-specified | Rollback, redeploy |

### Rollback Procedure

```bash
# 1. List available images
flyctl releases --image -a auto-ledger-staging

# 2. Identify image tag (e.g., main-442b9a8)
# 3. Trigger manual deployment
gh workflow run deploy-staging.yml -f image_tag=main-442b9a8

# 4. Verify deployment
curl https://auto-ledger-staging.fly.dev/actuator/health
```

## Troubleshooting

### Script Errors

| Error | Solution |
|-------|----------|
| `Command not found: supabase` | Run: `brew install supabase/tap/supabase` |
| `Not logged in to Supabase` | Run: `supabase login` |
| `Command not found: flyctl` | Run: `brew install flyctl` |
| `Not logged in to Fly.io` | Run: `fly auth login` |
| `Command not found: gh` | Install from https://cli.github.com |
| `jq: command not found` | Run: `brew install jq` |

### Authentication Issues

| Issue | Solution |
|-------|----------|
| "Invalid JWT" | Generate fresh token: `./scripts/clerk/testing/1-issue-jwt.sh` |
| "401 Unauthorized" | Check JWT expiration (tokens valid for 10 minutes) |
| "Issuer mismatch" | Verify `JWT_ISSUER_URI` matches Clerk domain |
| "Audience mismatch" | Check JWT template has correct `aud` claim |

### Deployment Issues

| Issue | Solution |
|-------|----------|
| Deploy fails - "image not found" | Wait for GitHub Actions build to complete |
| Deploy succeeds but app offline | Wait 30-60s for Fly.io machine to start (auto-scale from zero) |
| Health check fails | Check logs: `fly logs -a auto-ledger-staging` |

## Related Documentation

- **[PRD.md](../PRD.md)** - Complete project architecture and requirements
- **[CLAUDE.md](../CLAUDE.md)** - Development workflow and context management
- **[docs/CLERK-SETUP.md](../docs/CLERK-SETUP.md)** - Comprehensive Clerk setup guide
- **[STAGING-AUTH.md](../STAGING-AUTH.md)** - Quick reference for staging auth

## GitHub Issues

- **Issue #31**: Setup Supabase database integration
- **Issue #32**: Setup Fly.io infrastructure
- **Issue #33**: CI/CD pipeline for staging
- **Issue #54**: Backend Clerk authentication setup
