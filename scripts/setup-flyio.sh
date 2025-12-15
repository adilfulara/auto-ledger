#!/bin/bash
#
# Setup Fly.io application for Auto Ledger
#
# Usage:
#   ./scripts/setup-flyio.sh staging [--dry-run] [--force]
#   ./scripts/setup-flyio.sh production [--dry-run] [--force]
#
# Prerequisites:
#   - Fly CLI installed: brew install flyctl
#   - Logged in: fly auth login
#   - GitHub CLI installed: gh auth login
#   - Supabase database already created (Issue #31)
#
# Safety features:
#   - Checks if app already exists (idempotent)
#   - Asks for confirmation before creating/modifying
#   - Supports dry-run mode to preview changes
#   - Validates all prerequisites before starting
#   - Won't overwrite existing secrets without --force
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

  # Check Fly CLI
  if ! command -v flyctl &> /dev/null && ! command -v fly &> /dev/null; then
    log_error "Fly CLI not found. Install: brew install flyctl"
    exit 1
  fi

  # Check if logged in to Fly.io
  if ! flyctl auth whoami &> /dev/null; then
    log_error "Not logged in to Fly.io. Run: fly auth login"
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

  log_success "All prerequisites met"
}

#######################################
# Fly.io functions
#######################################
app_exists() {
  local app_name="$1"
  flyctl apps list 2>/dev/null | grep -q "^$app_name"
}

get_app_hostname() {
  local app_name="$1"
  echo "${app_name}.fly.dev"
}

create_flyio_app() {
  local app_name="$1"
  local region="$2"

  if $DRY_RUN; then
    log_dry_run "Would create Fly.io app: $app_name"
    log_dry_run "  Region: $region"
    log_dry_run "  Organization: personal"
    return 0
  fi

  log_info "Creating Fly.io app: $app_name..."

  # Create app
  if ! flyctl apps create "$app_name" --org personal 2>&1; then
    log_error "Failed to create Fly.io app"
    exit 1
  fi

  log_success "App created successfully"
}

set_flyio_secrets() {
  local app_name="$1"
  local env="$2"

  local secret_prefix
  if [[ "$env" == "staging" ]]; then
    secret_prefix="STAGING"
  else
    secret_prefix="PROD"
  fi

  # Get database credentials from GitHub secrets
  log_info "Retrieving database credentials from GitHub secrets..."

  # We can't read GitHub secrets directly, so we need to get them from Supabase
  # or have the user provide them
  if $DRY_RUN; then
    log_dry_run "Would set Fly.io secrets for $app_name:"
    log_dry_run "  DATABASE_URL (from ${secret_prefix}_DATABASE_URL)"
    log_dry_run "  DATABASE_USER (from ${secret_prefix}_DATABASE_USER)"
    log_dry_run "  DATABASE_PASSWORD (from ${secret_prefix}_DATABASE_PASSWORD)"
    return 0
  fi

  log_warning "Cannot retrieve GitHub secrets automatically (they're encrypted)"
  log_info "You need to set Fly.io secrets manually with database credentials"
  echo ""
  echo "Run these commands:"
  echo ""
  echo "  # Get database URL from Supabase dashboard or use the connection string"
  echo "  fly secrets set DATABASE_URL=\"jdbc:postgresql://db.YOUR-REF.supabase.co:5432/postgres\" -a $app_name"
  echo "  fly secrets set DATABASE_USER=\"postgres\" -a $app_name"
  echo "  fly secrets set DATABASE_PASSWORD=\"your-db-password\" -a $app_name"
  echo ""
}

create_fly_api_token() {
  local token_name="$1"

  if $DRY_RUN; then
    log_dry_run "Would create Fly API token: $token_name"
    log_dry_run "Would store as GitHub secret: FLY_API_TOKEN"
    return 0
  fi

  log_info "Creating Fly API token for GitHub Actions..."
  log_warning "You'll need to create this manually and store it in GitHub secrets"
  echo ""
  echo "Run these commands:"
  echo ""
  echo "  # Create deploy token scoped to the app"
  echo "  fly tokens create deploy -a $app_name -n $token_name"
  echo ""
  echo "  # Copy the token and store it"
  echo "  echo \"YOUR_FLY_TOKEN\" | gh secret set FLY_API_TOKEN"
  echo ""
}

#######################################
# Main execution
#######################################
main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Fly.io Application Setup"
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
  local app_name="auto-ledger-${ENVIRONMENT}"
  local config_file
  if [[ "$ENVIRONMENT" == "staging" ]]; then
    config_file="fly.staging.toml"
  else
    config_file="fly.production.toml"
  fi

  # Get region from config file
  local region
  if [[ -f "$config_file" ]]; then
    region=$(grep "primary_region" "$config_file" | cut -d'"' -f2)
  else
    log_warning "Config file $config_file not found, using default region lax"
    region="lax"
  fi

  log_info "App name: $app_name"
  log_info "Region: $region"
  log_info "Config: $config_file"

  # Check if app already exists
  if app_exists "$app_name"; then
    log_warning "Fly.io app '$app_name' already exists!"

    local hostname
    hostname=$(get_app_hostname "$app_name")

    echo ""
    echo "App details:"
    echo "  Name: $app_name"
    echo "  Hostname: $hostname"
    echo ""

    if ! $FORCE; then
      log_error "App already exists. Use --force to reconfigure secrets anyway"
      exit 1
    fi

    log_warning "Continuing with --force flag"
  else
    # App doesn't exist - create it
    log_info "App '$app_name' does not exist. Will create new app."
    echo ""

    if ! $DRY_RUN && ! $FORCE; then
      read -p "Create new Fly.io app '$app_name'? (yes/no): " -r
      if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        log_info "Aborted by user"
        exit 0
      fi
    fi

    # Create app
    create_flyio_app "$app_name" "$region"
  fi

  # Set secrets
  set_flyio_secrets "$app_name" "$ENVIRONMENT"

  # Create API token
  create_fly_api_token "github-actions-${ENVIRONMENT}"

  if ! $DRY_RUN; then
    echo ""
    echo "═══════════════════════════════════════════════════════"
    log_success "Fly.io $ENVIRONMENT app setup initiated!"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "App details:"
    echo "  Name: $app_name"
    echo "  Hostname: ${app_name}.fly.dev"
    echo "  Region: $region"
    echo ""
    echo "Next steps:"
    echo "  1. Set Fly.io secrets (see instructions above)"
    echo "  2. Create Fly API token (see instructions above)"
    echo "  3. Test deployment: flyctl deploy --config $config_file"
    echo "  4. Verify: https://${app_name}.fly.dev/actuator/health"
    echo ""
  fi

  if $DRY_RUN; then
    echo ""
    log_info "Dry run complete. No changes were made."
    log_info "Run without --dry-run to execute these changes."
  fi
}

# Run main function
main
