# Supabase Database Scripts

Scripts for managing Supabase PostgreSQL databases.

> **Note**: Currently used for staging. Production support will be added when needed.

## Scripts

| Script | Purpose |
|--------|---------|
| `setup.sh` | Create Supabase project and configure GitHub secrets |
| `cleanup.sh` | Delete Supabase project and remove GitHub secrets |

## Usage

### Create Staging Database

```bash
# Preview changes (dry-run)
./scripts/supabase/setup.sh staging --dry-run

# Create staging database
./scripts/supabase/setup.sh staging
```

### Delete Staging Database

```bash
# Preview deletion (dry-run)
./scripts/supabase/cleanup.sh staging --dry-run

# Delete staging database
./scripts/supabase/cleanup.sh staging
```

## Prerequisites

```bash
# Install Supabase CLI
brew install supabase/tap/supabase

# Login to Supabase (opens browser)
supabase login

# Install GitHub CLI
brew install gh

# Login to GitHub
gh auth login
```

## What setup.sh Does

1. ✅ Validates all prerequisites (CLI tools, authentication)
2. ✅ Checks if Supabase project already exists
3. ✅ Creates project if needed (with confirmation)
4. ✅ Generates secure database password
5. ✅ Configures GitHub secrets for CI/CD
6. ✅ Shows connection details for verification

## GitHub Secrets Created

The setup script configures these secrets for CI/CD:

- `STAGING_DATABASE_URL` - JDBC connection string
- `STAGING_DATABASE_USER` - Database username (postgres)
- `STAGING_DATABASE_PASSWORD` - Generated secure password

## Safety Features

- **Idempotent**: Safe to run multiple times
- **Dry-run mode**: Preview changes without making them
- **Confirmation prompts**: Explicit confirmations before creating resources
- **Prerequisite validation**: Checks for required tools before starting

## Related Documentation

- **Main Scripts README**: [../README.md](../README.md)
- **Issue #31**: Setup Supabase database integration
