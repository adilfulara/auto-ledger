# Auto Ledger - Infrastructure Setup Scripts

This directory contains automation scripts for setting up infrastructure components.

## Supabase Database Setup

**Script:** `setup-supabase.sh`

Automates the creation and configuration of Supabase PostgreSQL databases for staging and production environments.

### Prerequisites

```bash
# Install Supabase CLI
brew install supabase/tap/supabase

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
[DRY RUN]   Region: us-west-1
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
| `Supabase CLI not found` | Run: `brew install supabase/tap/supabase` |
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
  Region: us-west-1
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

## Fly.io Application Setup

**Script:** `setup-flyio.sh`

Automates the creation and configuration of Fly.io applications for staging and production environments.

### Prerequisites

```bash
# Install Fly CLI
brew install flyctl

# Login to Fly.io (one-time, opens browser)
fly auth login

# Verify login
fly auth whoami
```

### Usage

```bash
# Preview what will happen (safe, no changes)
./scripts/setup-flyio.sh staging --dry-run

# Create staging app
./scripts/setup-flyio.sh staging

# Create production app
./scripts/setup-flyio.sh production

# Force recreate/reconfigure
./scripts/setup-flyio.sh staging --force
```

### What It Does

1. ✅ Validates prerequisites (Fly CLI, authentication)
2. ✅ Checks if Fly.io app already exists
3. ✅ Creates app if needed (with confirmation)
4. ✅ Provides instructions for setting secrets
5. ✅ Provides instructions for creating API tokens

### Manual Steps Required

After running the script, you need to:

**1. Set Fly.io Secrets**
```bash
# Get database credentials from Supabase dashboard
fly secrets set DATABASE_URL="jdbc:postgresql://db.YOUR-REF.supabase.co:5432/postgres" -a auto-ledger-staging
fly secrets set DATABASE_USER="postgres" -a auto-ledger-staging
fly secrets set DATABASE_PASSWORD="your-db-password" -a auto-ledger-staging
```

**2. Create Fly API Token for GitHub Actions**
```bash
# Create deploy token scoped to the app
fly tokens create deploy -a auto-ledger-staging -n github-actions-staging

# Store in GitHub secrets
echo "YOUR_FLY_TOKEN" | gh secret set FLY_API_TOKEN
```

### Examples

#### First-time setup (staging)

```bash
$ ./scripts/setup-flyio.sh staging

═══════════════════════════════════════════════════════
  Auto Ledger - Fly.io Application Setup
  Environment: staging
═══════════════════════════════════════════════════════

ℹ Checking prerequisites...
✓ All prerequisites met
ℹ App name: auto-ledger-staging
ℹ Region: lax
ℹ Config: fly.staging.toml
ℹ App 'auto-ledger-staging' does not exist. Will create new app.

Create new Fly.io app 'auto-ledger-staging'? (yes/no): yes
ℹ Creating Fly.io app: auto-ledger-staging...
✓ App created successfully

Next steps:
  1. Set Fly.io secrets (see instructions above)
  2. Create Fly API token (see instructions above)
  3. Test deployment: flyctl deploy --config fly.staging.toml
  4. Verify: https://auto-ledger-staging.fly.dev/actuator/health
```

#### App already exists

```bash
$ ./scripts/setup-flyio.sh staging

⚠ Fly.io app 'auto-ledger-staging' already exists!

App details:
  Name: auto-ledger-staging
  Hostname: auto-ledger-staging.fly.dev

✗ App already exists. Use --force to reconfigure secrets anyway
```

## Fly.io Application Cleanup

**Script:** `cleanup-flyio.sh`

Safely deletes Fly.io applications.

### Usage

```bash
# Preview what will be deleted (safe)
./scripts/cleanup-flyio.sh staging --dry-run

# Delete staging app
./scripts/cleanup-flyio.sh staging

# Delete production app (requires --force)
./scripts/cleanup-flyio.sh production --force
```

### Safety Features

| Feature | Purpose |
|---------|---------|
| **Production protection** | Requires `--force` flag to delete production |
| **Explicit confirmation** | Must type "delete {environment}" to proceed |
| **`--dry-run` mode** | Preview deletions without making changes |
| **Idempotent** | Safe to run even if app already deleted |

### Related Issues

- Issue #32: Setup Fly.io infrastructure for staging deployment
- Issue #33: CI/CD pipeline for staging

## Docker Registry Setup (Fly.io)

The project uses **Fly's container registry** (`registry.fly.io`) for Docker images:

### Why Fly Registry?

- **No extra authentication**: Fly.io can pull images from its own registry without credentials
- **Simpler workflow**: No need for GitHub PAT or `--registry-auth` flags
- **Standard pattern**: Recommended approach in Fly.io documentation
- **Cost**: Included in Fly.io pricing (no GitHub Packages costs)

### How It Works

GitHub Actions workflow:
1. Builds Docker image from `./backend`
2. Logs into `registry.fly.io` using `FLY_API_TOKEN`
3. Pushes image with semantic tag (`main-<sha>` or `pr-<number>-<sha>`)
4. Deploys using `flyctl deploy --image registry.fly.io/auto-ledger-staging:<tag>`

### Required Secret

Only **one secret** is needed:

| Secret | Scope | Where to Get |
|--------|-------|--------------|
| `FLY_API_TOKEN` | Fly.io API access | `flyctl auth token` |

```bash
# Get your Fly.io token
flyctl auth token

# Store as GitHub secret
gh secret set FLY_API_TOKEN --body "your-fly-token"
```

### Migration from ghcr.io

Previously used GitHub Container Registry (ghcr.io) - migrated to Fly registry in PR #49:
- ✅ Removed dependency on `GH_PAT_PACKAGES` secret
- ✅ Simplified authentication flow
- ✅ Eliminated invalid `--registry-auth` flag error

## Staging Deployment & Rollback

The staging environment supports multiple deployment triggers:

### Deployment Triggers

| Trigger | Image Tag | Use Case |
|---------|-----------|----------|
| Push to feature branch (with open PR) | `pr-<number>-<sha>` | Test PR before merge |
| Push to `main` | `main-<sha>` | Post-merge verification |
| Manual dispatch | User-specified | Rollback, redeploy |

### Deploy Feature Branch

```bash
git checkout -b feat/issue-99-new-feature
git push origin feat/issue-99-new-feature
gh pr create  # Creates PR #45

# → GitHub Actions triggered
# → CI runs tests
# → If CI passes, deploys to staging
# → Image tagged: pr-45-abc1234
# → Preview at: https://auto-ledger-staging.fly.dev
```

When PR is merged and branch is deleted:
- Image is automatically cleaned up from ghcr.io
- Triggered by branch delete webhook
- Saves storage costs

### Deploy Main After Merge

```bash
gh pr merge 99

# → CI workflow runs
# → If tests pass, Deploy workflow triggered
# → Image tagged: staging + sha-<commit>
# → Preview at: https://auto-ledger-staging.fly.dev
```

### Understanding Fly.io Versions vs Image Tags

**Important**: Fly.io shows auto-incrementing version numbers (v1, v2, v3...) in the UI, but these are NOT the same as Docker image tags.

- **Fly version** (v11): Deployment counter for history tracking
- **Image tag** (main-442b9a8, pr-49-df3453f): Semantic tags for rollback

**For rollback, you need the IMAGE TAG from the Docker image column.**

### Rollback to Previous Version

**Step 1: List available images**

```bash
flyctl releases --image -a auto-ledger-staging

# Output:
# VERSION  STATUS    DOCKER IMAGE
# v11      complete  registry.fly.io/auto-ledger-staging:main-442b9a8
# v10      complete  registry.fly.io/auto-ledger-staging:pr-49-df3453f
```

**Step 2: Identify the image tag** from the rightmost column
(e.g., `main-442b9a8` or `pr-49-df3453f`)

**Step 3: Trigger manual deployment**

```bash
# Via GitHub UI:
# 1. Go to: https://github.com/adilfulara/auto-ledger/actions/workflows/deploy-staging.yml
# 2. Click "Run workflow"
# 3. Enter image tag: main-442b9a8
# 4. Click "Run workflow"

# Or via CLI:
gh workflow run deploy-staging.yml -f image_tag=main-442b9a8
```

**Step 4: Verify deployment**

```bash
curl https://auto-ledger-staging.fly.dev/actuator/health
```

### Image Retention Policy

Images are stored in Fly's registry (`registry.fly.io`):

- **Retention**: Images persist until manually deleted
- **Storage**: No GitHub Packages costs (using Fly's registry)
- **Cleanup**: To remove old images, use `flyctl` commands:

```bash
# List all images (not just deployed ones)
flyctl image show -a auto-ledger-staging

# Delete specific image version (if needed)
# Note: Requires manual registry API calls - contact Fly.io support for bulk cleanup
```

### Troubleshooting Deployments

| Issue | Solution |
|-------|----------|
| Deploy fails - "image not found" | Check GitHub Packages - image may not have built yet |
| Deploy fails - "registry auth failed" | Verify `GH_PAT_PACKAGES` secret is valid and not expired |
| Deploy succeeds but app offline | Wait 30-60 seconds for Fly.io machine to start (auto-scale from zero) |
| Health check fails | Check logs: `fly logs -a auto-ledger-staging` |
