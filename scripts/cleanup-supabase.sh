#!/bin/bash
#
# Cleanup Supabase database for Auto Ledger
#
# Usage:
#   ./scripts/cleanup-supabase.sh staging [--dry-run] [--force]
#   ./scripts/cleanup-supabase.sh production [--dry-run] [--force]
#
# Prerequisites:
#   - Supabase CLI installed: npm install -g supabase
#   - Logged in: supabase login
#   - GitHub CLI installed: gh auth login
#
# Safety features:
#   - Requires explicit confirmation before deletion
#   - Supports dry-run mode to preview deletions
#   - Checks if resources exist before attempting deletion (idempotent)
#   - Production environment requires --force flag
#   - Shows what will be deleted before proceeding
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

  # Extra safety for production
  if [[ "$ENVIRONMENT" == "production" ]] && ! $FORCE; then
    log_error "Deleting PRODUCTION requires --force flag"
    echo "Usage: $0 production --force [--dry-run]"
    exit 1
  fi
}

check_prerequisites() {
  log_info "Checking prerequisites..."

  # Check Supabase CLI
  if ! command -v supabase &> /dev/null; then
    log_error "Supabase CLI not found. Install: npm install -g supabase"
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
project_exists() {
  local project_name="$1"
  supabase projects list --output json 2>/dev/null | jq -e ".[] | select(.name == \"$project_name\")" > /dev/null
}

get_project_ref() {
  local project_name="$1"
  supabase projects list --output json 2>/dev/null | jq -r ".[] | select(.name == \"$project_name\") | .id"
}

get_project_region() {
  local project_name="$1"
  supabase projects list --output json 2>/dev/null | jq -r ".[] | select(.name == \"$project_name\") | .region"
}

delete_supabase_project() {
  local project_ref="$1"

  if $DRY_RUN; then
    log_dry_run "Would delete Supabase project: $project_ref"
    return 0
  fi

  log_info "Deleting Supabase project: $project_ref..."

  if ! supabase projects delete "$project_ref"; then
    log_error "Failed to delete Supabase project"
    log_warning "Project may have already been deleted or requires manual deletion"
    log_info "Visit: https://supabase.com/dashboard/project/$project_ref/settings/general"
    return 1
  fi

  log_success "Project deleted successfully"
}

#######################################
# GitHub Secrets functions
#######################################
github_secret_exists() {
  local secret_name="$1"
  gh secret list 2>/dev/null | grep -q "^$secret_name"
}

delete_github_secrets() {
  local env="$1"

  local secret_prefix
  if [[ "$env" == "staging" ]]; then
    secret_prefix="STAGING"
  else
    secret_prefix="PROD"
  fi

  local url_secret="${secret_prefix}_DATABASE_URL"
  local user_secret="${secret_prefix}_DATABASE_USER"
  local pass_secret="${secret_prefix}_DATABASE_PASSWORD"

  local secrets_deleted=0

  if $DRY_RUN; then
    log_dry_run "Would delete GitHub secrets:"
    if github_secret_exists "$url_secret"; then
      log_dry_run "  $url_secret"
      secrets_deleted=$((secrets_deleted + 1))
    fi
    if github_secret_exists "$user_secret"; then
      log_dry_run "  $user_secret"
      secrets_deleted=$((secrets_deleted + 1))
    fi
    if github_secret_exists "$pass_secret"; then
      log_dry_run "  $pass_secret"
      secrets_deleted=$((secrets_deleted + 1))
    fi

    if [[ $secrets_deleted -eq 0 ]]; then
      log_dry_run "  (No secrets found)"
    fi
    return 0
  fi

  log_info "Deleting GitHub secrets..."

  if github_secret_exists "$url_secret"; then
    gh secret delete "$url_secret" --confirm
    log_success "Deleted $url_secret"
    secrets_deleted=$((secrets_deleted + 1))
  fi

  if github_secret_exists "$user_secret"; then
    gh secret delete "$user_secret" --confirm
    log_success "Deleted $user_secret"
    secrets_deleted=$((secrets_deleted + 1))
  fi

  if github_secret_exists "$pass_secret"; then
    gh secret delete "$pass_secret" --confirm
    log_success "Deleted $pass_secret"
    secrets_deleted=$((secrets_deleted + 1))
  fi

  if [[ $secrets_deleted -eq 0 ]]; then
    log_info "No GitHub secrets found (already deleted)"
  else
    log_success "Deleted $secrets_deleted GitHub secret(s)"
  fi
}

#######################################
# Main execution
#######################################
main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Supabase Database Cleanup"
  echo "  Environment: $ENVIRONMENT"
  if $DRY_RUN; then
    echo "  Mode: DRY RUN (no changes will be made)"
  fi
  if [[ "$ENVIRONMENT" == "production" ]]; then
    echo -e "  ${RED}WARNING: PRODUCTION ENVIRONMENT${NC}"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""

  # Validate inputs
  validate_environment
  check_prerequisites

  # Configuration
  local project_name="auto-ledger-${ENVIRONMENT}"

  # Check current state
  log_info "Checking current state..."
  echo ""

  local project_ref=""
  local project_region=""
  local project_found=false

  if project_exists "$project_name"; then
    project_found=true
    project_ref=$(get_project_ref "$project_name")
    project_region=$(get_project_region "$project_name")

    echo "Supabase Project:"
    echo "  Name: $project_name"
    echo "  Ref: $project_ref"
    echo "  Region: $project_region"
    echo "  Host: db.${project_ref}.supabase.co"
  else
    echo "Supabase Project:"
    echo "  Status: Not found (already deleted or never created)"
  fi

  echo ""

  local secret_prefix
  if [[ "$ENVIRONMENT" == "staging" ]]; then
    secret_prefix="STAGING"
  else
    secret_prefix="PROD"
  fi

  echo "GitHub Secrets:"
  local secrets_found=0
  for secret_name in "${secret_prefix}_DATABASE_URL" "${secret_prefix}_DATABASE_USER" "${secret_prefix}_DATABASE_PASSWORD"; do
    if github_secret_exists "$secret_name"; then
      echo "  ✓ $secret_name"
      secrets_found=$((secrets_found + 1))
    fi
  done

  if [[ $secrets_found -eq 0 ]]; then
    echo "  (None found)"
  fi

  echo ""

  # Check if there's anything to delete
  if ! $project_found && [[ $secrets_found -eq 0 ]]; then
    log_success "Environment already clean - nothing to delete"
    exit 0
  fi

  # Show what will be deleted
  echo "The following resources will be deleted:"
  echo ""
  if $project_found; then
    echo "  • Supabase project: $project_name ($project_ref)"
    echo "    - All databases and data will be permanently deleted"
    echo "    - This action cannot be undone"
  fi
  if [[ $secrets_found -gt 0 ]]; then
    echo "  • GitHub secrets: ${secrets_found} secret(s)"
  fi
  echo ""

  # Confirmation prompt
  if ! $DRY_RUN && ! $FORCE; then
    log_warning "This is a destructive operation!"

    if [[ "$ENVIRONMENT" == "production" ]]; then
      echo ""
      log_error "You are about to delete the PRODUCTION environment!"
      echo ""
    fi

    read -p "Type 'delete $ENVIRONMENT' to confirm: " -r
    local expected="delete $ENVIRONMENT"
    if [[ "$REPLY" != "$expected" ]]; then
      log_info "Aborted by user (confirmation text did not match)"
      exit 0
    fi

    echo ""
  fi

  # Delete Supabase project
  if $project_found; then
    delete_supabase_project "$project_ref" || true
  fi

  # Delete GitHub secrets
  if [[ $secrets_found -gt 0 ]]; then
    delete_github_secrets "$ENVIRONMENT"
  fi

  # Summary
  echo ""
  echo "═══════════════════════════════════════════════════════"
  if $DRY_RUN; then
    log_info "Dry run complete. No changes were made."
    log_info "Run without --dry-run to execute these deletions."
  else
    log_success "Cleanup complete for $ENVIRONMENT environment"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""
}

# Run main function
main
