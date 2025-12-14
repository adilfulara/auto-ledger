# Auto Ledger - Infrastructure Setup Scripts

This directory contains automation scripts for setting up infrastructure components.

## Supabase Database Setup

**Script:** `setup-supabase.sh`

Automates the creation and configuration of Supabase PostgreSQL databases for staging and production environments.

### Prerequisites

```bash
# Install Supabase CLI
npm install -g supabase

# Login to Supabase (one-time, opens browser)
supabase login

# Install GitHub CLI (if not already installed)
# Ubuntu/Debian: sudo apt install gh
# macOS: brew install gh

# Login to GitHub
gh auth login
```

### Usage

```bash
# Preview what will happen (safe, no changes)
./scripts/setup-supabase.sh staging --dry-run

# Create staging database
./scripts/setup-supabase.sh staging

# Create production database
./scripts/setup-supabase.sh production

# Force overwrite existing secrets
./scripts/setup-supabase.sh staging --force
```

### Safety Features

| Feature | Purpose |
|---------|---------|
| **Idempotent** | Safe to run multiple times - checks if project exists first |
| **Confirmation prompts** | Asks before creating resources or overwriting secrets |
| **`--dry-run` mode** | Preview changes without making them |
| **`--force` flag** | Required to overwrite existing secrets |
| **Prerequisite validation** | Checks for required tools before starting |
| **Error handling** | Exits cleanly on failures with helpful messages |
| **Colored output** | Easy to spot errors, warnings, and success messages |

### What It Does

1. ✅ Validates all prerequisites (CLI tools, authentication)
2. ✅ Checks if Supabase project already exists
3. ✅ Creates project if needed (with confirmation)
4. ✅ Generates secure database password
5. ✅ Configures GitHub secrets for CI/CD
6. ✅ Shows connection details for verification

### Examples

#### First-time setup (staging)

```bash
$ ./scripts/setup-supabase.sh staging

═══════════════════════════════════════════════════════
  Auto Ledger - Supabase Database Setup
  Environment: staging
═══════════════════════════════════════════════════════

ℹ Checking prerequisites...
✓ All prerequisites met
ℹ Organization ID: abc123def456
ℹ Project 'auto-ledger-staging' does not exist. Will create new project.

Create new Supabase project 'auto-ledger-staging'? (yes/no): yes
ℹ Creating Supabase project: auto-ledger-staging...
✓ Project created successfully
ℹ Waiting for project to be ready (this may take 30-60 seconds)...
ℹ Setting GitHub secrets...
✓ GitHub secrets configured

═══════════════════════════════════════════════════════
✓ Supabase staging database setup complete!
═══════════════════════════════════════════════════════

Connection details:
  Host: db.xyz789.supabase.co
  Port: 5432
  Database: postgres
  User: postgres
  Password: [stored in GitHub secrets]

GitHub secrets configured:
  STAGING_DATABASE_URL
  STAGING_DATABASE_USER
  STAGING_DATABASE_PASSWORD

Next steps:
  1. Verify connection: psql jdbc:postgresql://db.xyz789.supabase.co:5432/postgres
  2. Continue to Issue #32 (Fly.io setup)
```

#### Dry run (preview changes)

```bash
$ ./scripts/setup-supabase.sh staging --dry-run

[DRY RUN] Would create Supabase project: auto-ledger-staging
[DRY RUN]   Organization: abc123def456
[DRY RUN]   Region: us-east-1
[DRY RUN]   Plan: free
[DRY RUN] Would set GitHub secrets:
[DRY RUN]   STAGING_DATABASE_URL = jdbc:postgresql://...
[DRY RUN]   STAGING_DATABASE_USER = postgres
[DRY RUN]   STAGING_DATABASE_PASSWORD = [REDACTED]

ℹ Dry run complete. No changes were made.
```

#### Project already exists

```bash
$ ./scripts/setup-supabase.sh staging

⚠ Supabase project 'auto-ledger-staging' already exists!

Project details:
  Name: auto-ledger-staging
  Ref: xyz789
  Host: db.xyz789.supabase.co

✗ Project already exists. Use --force to reconfigure GitHub secrets anyway
```

#### Force reconfigure secrets

```bash
$ ./scripts/setup-supabase.sh staging --force

⚠ Supabase project 'auto-ledger-staging' already exists!
⚠ Continuing with --force flag (will update GitHub secrets only)
⚠ Cannot retrieve existing database password from Supabase
⚠ You must provide the existing password for GitHub secrets

Enter existing database password for auto-ledger-staging: ********
ℹ Setting GitHub secrets...
✓ GitHub secrets configured
```

### Troubleshooting

| Error | Solution |
|-------|----------|
| `Supabase CLI not found` | Run: `npm install -g supabase` |
| `Not logged in to Supabase` | Run: `supabase login` |
| `GitHub CLI not found` | Install from https://cli.github.com |
| `jq not found` | Ubuntu: `sudo apt install jq`, macOS: `brew install jq` |
| `Project already exists` | Use `--dry-run` to check state, or `--force` to update secrets |
| `Failed to get project reference` | Project is still provisioning - wait 1-2 minutes and rerun |

### Environment Variables Set

#### Staging
- `STAGING_DATABASE_URL` - JDBC connection string
- `STAGING_DATABASE_USER` - Database username (postgres)
- `STAGING_DATABASE_PASSWORD` - Generated secure password

#### Production
- `PROD_DATABASE_URL` - JDBC connection string
- `PROD_DATABASE_USER` - Database username (postgres)
- `PROD_DATABASE_PASSWORD` - Generated secure password

## Supabase Database Cleanup

**Script:** `cleanup-supabase.sh`

Safely deletes Supabase projects and removes GitHub secrets. Useful for tearing down test environments or resetting state.

### Usage

```bash
# Preview what will be deleted (safe, no changes)
./scripts/cleanup-supabase.sh staging --dry-run

# Delete staging database
./scripts/cleanup-supabase.sh staging

# Delete production database (requires --force)
./scripts/cleanup-supabase.sh production --force
```

### Safety Features

| Feature | Purpose |
|---------|---------|
| **Production protection** | Requires `--force` flag to delete production |
| **Explicit confirmation** | Must type "delete {environment}" to proceed |
| **`--dry-run` mode** | Preview deletions without making changes |
| **Idempotent** | Safe to run even if resources already deleted |
| **State checking** | Shows what exists before deletion |
| **Graceful handling** | Handles already-deleted resources cleanly |

### What It Deletes

1. ✅ Supabase project (if exists)
   - All databases and data (permanent deletion)
   - Cannot be undone
2. ✅ GitHub secrets (if exist)
   - `{ENV}_DATABASE_URL`
   - `{ENV}_DATABASE_USER`
   - `{ENV}_DATABASE_PASSWORD`

### Examples

#### Preview cleanup (staging)

```bash
$ ./scripts/cleanup-supabase.sh staging --dry-run

═══════════════════════════════════════════════════════
  Auto Ledger - Supabase Database Cleanup
  Environment: staging
  Mode: DRY RUN (no changes will be made)
═══════════════════════════════════════════════════════

ℹ Checking current state...

Supabase Project:
  Name: auto-ledger-staging
  Ref: xyz789
  Region: us-east-1
  Host: db.xyz789.supabase.co

GitHub Secrets:
  ✓ STAGING_DATABASE_URL
  ✓ STAGING_DATABASE_USER
  ✓ STAGING_DATABASE_PASSWORD

The following resources will be deleted:

  • Supabase project: auto-ledger-staging (xyz789)
    - All databases and data will be permanently deleted
    - This action cannot be undone
  • GitHub secrets: 3 secret(s)

[DRY RUN] Would delete Supabase project: xyz789
[DRY RUN] Would delete GitHub secrets:
[DRY RUN]   STAGING_DATABASE_URL
[DRY RUN]   STAGING_DATABASE_USER
[DRY RUN]   STAGING_DATABASE_PASSWORD

ℹ Dry run complete. No changes were made.
```

#### Delete staging environment

```bash
$ ./scripts/cleanup-supabase.sh staging

⚠ This is a destructive operation!

Type 'delete staging' to confirm: delete staging

ℹ Deleting Supabase project: xyz789...
✓ Project deleted successfully
ℹ Deleting GitHub secrets...
✓ Deleted STAGING_DATABASE_URL
✓ Deleted STAGING_DATABASE_USER
✓ Deleted STAGING_DATABASE_PASSWORD
✓ Deleted 3 GitHub secret(s)

═══════════════════════════════════════════════════════
✓ Cleanup complete for staging environment
═══════════════════════════════════════════════════════
```

#### Attempt to delete production (without --force)

```bash
$ ./scripts/cleanup-supabase.sh production

✗ Deleting PRODUCTION requires --force flag
Usage: ./scripts/cleanup-supabase.sh production --force [--dry-run]
```

#### Delete production (with --force)

```bash
$ ./scripts/cleanup-supabase.sh production --force

═══════════════════════════════════════════════════════
  Auto Ledger - Supabase Database Cleanup
  Environment: production
  WARNING: PRODUCTION ENVIRONMENT
═══════════════════════════════════════════════════════

⚠ This is a destructive operation!

✗ You are about to delete the PRODUCTION environment!

Type 'delete production' to confirm: delete production

ℹ Deleting Supabase project: abc123...
✓ Project deleted successfully
```

#### Already clean environment

```bash
$ ./scripts/cleanup-supabase.sh staging

Supabase Project:
  Status: Not found (already deleted or never created)

GitHub Secrets:
  (None found)

✓ Environment already clean - nothing to delete
```

### When to Use Cleanup

- **Reset staging:** Before testing deployment from scratch
- **Cost management:** Free tier has 2 project limit
- **Development:** Clean up after testing automation scripts
- **Environment refresh:** Delete and recreate with new configuration

### Related Issues

- Issue #31: Setup Supabase database integration for staging
- Issue #32: Setup Fly.io infrastructure
