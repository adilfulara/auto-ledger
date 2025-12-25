# Fly.io Infrastructure Scripts

Scripts for managing Fly.io application deployment infrastructure.

> **Note**: Currently used for staging. Production support will be added when needed.

## Scripts

| Script | Purpose |
|--------|---------|
| `setup.sh` | Create Fly.io application for staging |
| `cleanup.sh` | Delete Fly.io application (requires confirmation) |

## Usage

### Create Staging Application

```bash
# Preview changes (dry-run)
./scripts/flyio/setup.sh staging --dry-run

# Create staging app
./scripts/flyio/setup.sh staging
```

### Delete Staging Application

```bash
# Preview deletion (dry-run)
./scripts/flyio/cleanup.sh staging --dry-run

# Delete staging app
./scripts/flyio/cleanup.sh staging
```

## Prerequisites

```bash
# Install Fly CLI
brew install flyctl

# Login to Fly.io
fly auth login

# Verify login
fly auth whoami
```

## What setup.sh Does

1. ✅ Validates prerequisites (Fly CLI, authentication)
2. ✅ Checks if app already exists
3. ✅ Creates app if needed (with confirmation)
4. ✅ Provides instructions for manual setup steps

## Manual Steps After Setup

After running `setup.sh`, you need to:

### 1. Set Fly.io Secrets

```bash
# Database credentials (from Supabase)
fly secrets set DATABASE_URL="jdbc:postgresql://..." -a auto-ledger-staging
fly secrets set DATABASE_USER="postgres" -a auto-ledger-staging
fly secrets set DATABASE_PASSWORD="..." -a auto-ledger-staging
```

### 2. Create Fly API Token for GitHub Actions

```bash
# Create deploy token
fly tokens create deploy -a auto-ledger-staging

# Store in GitHub secrets
gh secret set FLY_API_TOKEN
```

## Safety Features

- **Dry-run mode**: Preview changes without making them
- **Idempotent**: Safe to run multiple times
- **Confirmation prompts**: Explicit confirmations for destructive operations

## Related Documentation

- **Main Scripts README**: [../README.md](../README.md)
- **Issue #32**: Setup Fly.io infrastructure
