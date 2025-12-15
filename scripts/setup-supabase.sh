#!/bin/bash
#
# Setup Supabase database for Auto Ledger
#
# Usage:
#   ./scripts/setup-supabase.sh staging [--dry-run] [--force]
#   ./scripts/setup-supabase.sh production [--dry-run] [--force]
#
# Prerequisites:
#   - Supabase CLI installed: brew install supabase/tap/supabase
#   - Logged in: supabase login
#   - GitHub CLI installed: gh auth login
#
# Safety features:
#   - Checks if project already exists (idempotent)
#   - Asks for confirmation before creating/modifying
#   - Supports dry-run mode to preview changes
#   - Validates all prerequisites before starting
#   - Won't overwrite existing GitHub secrets without --force
#

set -euo pipefail  # Exit on error, undefined vars, pipe failures

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-}"
DRY_RUN=false
FORCE=false

# Parse arguments
for arg in "$@"; do
  case $arg in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --force)
      FORCE=true
      shift
      ;;
  esac
done

#######################################
# Print functions
#######################################
log_info() {
  echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
  echo -e "${GREEN}✓${NC} $1"
}

log_warning() {
  echo -e "${YELLOW}⚠${NC} $1"
}

log_error() {
  echo -e "${RED}✗${NC} $1"
}

log_dry_run() {
  echo -e "${YELLOW}[DRY RUN]${NC} $1"
}

#######################################
# Validation functions
#######################################
validate_environment() {
  if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
    log_error "Invalid environment. Must be 'staging' or 'production'"
    echo "Usage: $0 <staging|production> [--dry-run] [--force]"
    exit 1
  fi
}

check_prerequisites() {
  log_info "Checking prerequisites..."

  # Check Supabase CLI
  if ! command -v supabase &> /dev/null; then
    log_error "Supabase CLI not found. Install: brew install supabase/tap/supabase"
    exit 1
  fi

  # Check if logged in to Supabase
  if ! supabase projects list &> /dev/null; then
    log_error "Not logged in to Supabase. Run: supabase login"
    exit 1
  fi

  # Check GitHub CLI
  if ! command -v gh &> /dev/null; then
    log_error "GitHub CLI not found. Install: https://cli.github.com"
    exit 1
  fi

  # Check if logged in to GitHub
  if ! gh auth status &> /dev/null; then
    log_error "Not logged in to GitHub. Run: gh auth login"
    exit 1
  fi

  # Check for required tools
  if ! command -v jq &> /dev/null; then
    log_error "jq not found. Install: sudo apt-get install jq"
    exit 1
  fi

  log_success "All prerequisites met"
}

#######################################
# Supabase functions
#######################################
get_org_id() {
  local org_id
  org_id=$(supabase orgs list --output json 2>/dev/null | jq -r '.[0].id' || echo "")

  if [[ -z "$org_id" || "$org_id" == "null" ]]; then
    log_error "Could not find Supabase organization. Create one at https://supabase.com/dashboard"
    exit 1
  fi

  echo "$org_id"
}

project_exists() {
  local project_name="$1"
  supabase projects list --output json 2>/dev/null | jq -e ".[]? | select(.name == \"$project_name\")" > /dev/null 2>&1
}

get_project_ref() {
  local project_name="$1"
  supabase projects list --output json 2>/dev/null | jq -r ".[]? | select(.name == \"$project_name\") | .id" 2>/dev/null
}

create_supabase_project() {
  local project_name="$1"
  local db_password="$2"
  local org_id="$3"

  if $DRY_RUN; then
    log_dry_run "Would create Supabase project: $project_name"
    log_dry_run "  Organization: $org_id"
    log_dry_run "  Region: us-west-1"
    log_dry_run "  Plan: free"
    return 0
  fi

  log_info "Creating Supabase project: $project_name..."

  # Create project (this outputs JSON with project details)
  if ! supabase projects create "$project_name" \
    --org-id "$org_id" \
    --db-password "$db_password" \
    --region us-west-1 \
    --plan free; then
    log_error "Failed to create Supabase project"
    exit 1
  fi

  log_success "Project created successfully"

  # Wait for project to be fully provisioned
  log_info "Waiting for project to be ready (this may take 30-60 seconds)..."
  sleep 30
}

#######################################
# GitHub Secrets functions
#######################################
github_secret_exists() {
  local secret_name="$1"
  gh secret list 2>/dev/null | grep -q "^$secret_name"
}

set_github_secrets() {
  local env="$1"
  local db_url="$2"
  local db_password="$3"

  local secret_prefix
  if [[ "$env" == "staging" ]]; then
    secret_prefix="STAGING"
  else
    secret_prefix="PROD"
  fi

  local url_secret="${secret_prefix}_DATABASE_URL"
  local user_secret="${secret_prefix}_DATABASE_USER"
  local pass_secret="${secret_prefix}_DATABASE_PASSWORD"

  # Check if secrets already exist
  local secrets_exist=false
  if github_secret_exists "$url_secret" || \
     github_secret_exists "$user_secret" || \
     github_secret_exists "$pass_secret"; then
    secrets_exist=true
  fi

  if $secrets_exist && ! $FORCE; then
    log_warning "GitHub secrets already exist for $env environment"
    log_warning "Use --force to overwrite existing secrets"
    echo ""
    echo "Existing secrets:"
    gh secret list | grep "^${secret_prefix}_DATABASE"
    echo ""
    read -p "Overwrite existing secrets? (yes/no): " -r
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
      log_info "Skipping GitHub secrets update"
      return 0
    fi
  fi

  if $DRY_RUN; then
    log_dry_run "Would set GitHub secrets:"
    log_dry_run "  $url_secret = $db_url"
    log_dry_run "  $user_secret = postgres"
    log_dry_run "  $pass_secret = [REDACTED]"
    return 0
  fi

  log_info "Setting GitHub secrets..."

  echo "$db_url" | gh secret set "$url_secret"
  echo "postgres" | gh secret set "$user_secret"
  echo "$db_password" | gh secret set "$pass_secret"

  log_success "GitHub secrets configured"
}

#######################################
# Main execution
#######################################
main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Supabase Database Setup"
  echo "  Environment: $ENVIRONMENT"
  if $DRY_RUN; then
    echo "  Mode: DRY RUN (no changes will be made)"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""

  # Validate inputs
  validate_environment
  check_prerequisites

  # Configuration
  local project_name="auto-ledger-${ENVIRONMENT}"
  local org_id
  org_id=$(get_org_id)

  log_info "Organization ID: $org_id"

  # Check if project already exists
  if project_exists "$project_name"; then
    log_warning "Supabase project '$project_name' already exists!"

    local project_ref
    project_ref=$(get_project_ref "$project_name")

    echo ""
    echo "Project details:"
    echo "  Name: $project_name"
    echo "  Ref: $project_ref"
    echo "  Host: db.${project_ref}.supabase.co"
    echo ""

    if ! $FORCE; then
      log_error "Project already exists. Use --force to reconfigure GitHub secrets anyway"
      exit 1
    fi

    log_warning "Continuing with --force flag (will update GitHub secrets only)"

    # Use existing project
    local db_host="db.${project_ref}.supabase.co"
    local db_url="jdbc:postgresql://${db_host}:5432/postgres"

    # We can't retrieve the existing password, so warn user
    log_warning "Cannot retrieve existing database password from Supabase"
    log_warning "You must provide the existing password for GitHub secrets"
    echo ""

    if $DRY_RUN; then
      local db_password="<existing-password>"
    else
      read -sp "Enter existing database password for $project_name: " db_password
      echo ""
    fi

    set_github_secrets "$ENVIRONMENT" "$db_url" "$db_password"

  else
    # Project doesn't exist - create it
    log_info "Project '$project_name' does not exist. Will create new project."
    echo ""

    if ! $DRY_RUN && ! $FORCE; then
      read -p "Create new Supabase project '$project_name'? (yes/no): " -r
      if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Aborted by user"
        exit 0
      fi
    fi

    # Generate secure password
    local db_password
    if $DRY_RUN; then
      db_password="<generated-password>"
    else
      db_password=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
    fi

    # Create project
    create_supabase_project "$project_name" "$db_password" "$org_id"

    # Get project details
    if ! $DRY_RUN; then
      local project_ref
      project_ref=$(get_project_ref "$project_name")

      if [[ -z "$project_ref" ]]; then
        log_error "Failed to get project reference. Project may still be provisioning."
        log_info "Run this script again in a few minutes to configure GitHub secrets"
        exit 1
      fi

      local db_host="db.${project_ref}.supabase.co"
      local db_url="jdbc:postgresql://${db_host}:5432/postgres"

      # Set GitHub secrets
      set_github_secrets "$ENVIRONMENT" "$db_url" "$db_password"

      # Show summary
      echo ""
      echo "═══════════════════════════════════════════════════════"
      log_success "Supabase $ENVIRONMENT database setup complete!"
      echo "═══════════════════════════════════════════════════════"
      echo ""
      echo "Connection details:"
      echo "  Host: $db_host"
      echo "  Port: 5432"
      echo "  Database: postgres"
      echo "  User: postgres"
      echo "  Password: [stored in GitHub secrets]"
      echo ""
      echo "GitHub secrets configured:"
      echo "  ${ENVIRONMENT^^}_DATABASE_URL"
      echo "  ${ENVIRONMENT^^}_DATABASE_USER"
      echo "  ${ENVIRONMENT^^}_DATABASE_PASSWORD"
      echo ""
      echo "Next steps:"
      echo "  1. Verify connection: psql $db_url"
      echo "  2. Continue to Issue #32 (Fly.io setup)"
      echo ""
    fi
  fi

  if $DRY_RUN; then
    echo ""
    log_info "Dry run complete. No changes were made."
    log_info "Run without --dry-run to execute these changes."
  fi
}

# Run main function
main
