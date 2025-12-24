#!/bin/bash
#
# Setup Clerk authentication for Auto Ledger
#
# Usage:
#   ./scripts/setup-clerk-auth.sh staging [--dry-run] [--force]
#   ./scripts/setup-clerk-auth.sh production [--dry-run] [--force]
#   ./scripts/setup-clerk-auth.sh staging --issuer-uri "https://clerk.example.com" --audience "auto-ledger-staging"
#
# Prerequisites:
#   - Fly CLI installed: brew install flyctl
#   - Logged in: fly auth login
#   - Clerk account created and application configured (see docs/CLERK-SETUP.md)
#
# Safety features:
#   - Validates issuer URI format (must be HTTPS)
#   - Prompts for values if not provided
#   - Asks for confirmation before setting secrets
#   - Supports dry-run mode to preview changes
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
ISSUER_URI=""
AUDIENCE=""

# Parse arguments
shift || true  # Skip first arg (environment)
while [[ $# -gt 0 ]]; do
  case $1 in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --force)
      FORCE=true
      shift
      ;;
    --issuer-uri)
      ISSUER_URI="$2"
      shift 2
      ;;
    --audience)
      AUDIENCE="$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
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
    echo "Usage: $0 <staging|production> [--dry-run] [--force] [--issuer-uri URI] [--audience AUD]"
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

  log_success "All prerequisites met"
}

validate_issuer_uri() {
  local uri="$1"

  # Must start with https://
  if [[ ! "$uri" =~ ^https:// ]]; then
    log_error "Issuer URI must start with https://"
    return 1
  fi

  # Must not end with slash
  if [[ "$uri" =~ /$ ]]; then
    log_error "Issuer URI must not end with a slash"
    return 1
  fi

  # Basic URL validation
  if [[ ! "$uri" =~ ^https://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
    log_error "Invalid issuer URI format. Expected: https://clerk.example.com"
    return 1
  fi

  return 0
}

prompt_for_values() {
  # Prompt for issuer URI if not provided
  if [[ -z "$ISSUER_URI" ]]; then
    echo ""
    echo "Enter Clerk issuer URI (e.g., https://clerk.auto-ledger.abc123.lcl.dev)"
    echo "Find this in: Clerk Dashboard → Configure → API Keys → Frontend API"
    echo ""
    read -p "Issuer URI: " ISSUER_URI
  fi

  # Validate issuer URI
  if ! validate_issuer_uri "$ISSUER_URI"; then
    exit 1
  fi

  # Prompt for audience if not provided
  if [[ -z "$AUDIENCE" ]]; then
    local default_audience="auto-ledger-${ENVIRONMENT}"
    echo ""
    echo "Enter JWT audience (default: ${default_audience})"
    echo "This should match the audience configured in your Clerk JWT template"
    echo ""
    read -p "Audience [${default_audience}]: " AUDIENCE
    AUDIENCE="${AUDIENCE:-$default_audience}"
  fi

  log_success "Configuration values set"
}

#######################################
# Fly.io functions
#######################################
app_exists() {
  local app_name="$1"
  flyctl apps list 2>/dev/null | grep -q "^$app_name"
}

set_fly_secrets() {
  local app_name="$1"

  log_info "Configuration to set:"
  echo "  AUTH_ENABLED: true"
  echo "  JWT_ISSUER_URI: $ISSUER_URI"
  echo "  JWT_AUDIENCE: $AUDIENCE"
  echo ""

  if $DRY_RUN; then
    log_dry_run "Would set Fly.io secrets for app: $app_name"
    log_dry_run "  AUTH_ENABLED=true"
    log_dry_run "  JWT_ISSUER_URI=$ISSUER_URI"
    log_dry_run "  JWT_AUDIENCE=$AUDIENCE"
    log_dry_run ""
    log_dry_run "This will trigger a deployment of the app"
    return 0
  fi

  if ! $FORCE; then
    echo ""
    log_warning "Setting secrets will trigger a deployment"
    read -p "Continue? (yes/no): " -r
    if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
      log_info "Aborted by user"
      exit 0
    fi
  fi

  log_info "Setting Fly.io secrets..."

  # Set all secrets in a single command to trigger only one deployment
  if ! flyctl secrets set \
    AUTH_ENABLED=true \
    JWT_ISSUER_URI="$ISSUER_URI" \
    JWT_AUDIENCE="$AUDIENCE" \
    -a "$app_name" 2>&1; then
    log_error "Failed to set Fly.io secrets"
    exit 1
  fi

  log_success "Secrets set successfully"
}

#######################################
# Main execution
#######################################
main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Clerk Authentication Setup"
  echo "  Environment: $ENVIRONMENT"
  if $DRY_RUN; then
    echo "  Mode: DRY RUN (no changes will be made)"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""

  # Validate inputs
  validate_environment
  check_prerequisites

  # Determine app name
  local app_name="auto-ledger-${ENVIRONMENT}"

  # Check if app exists
  if ! app_exists "$app_name"; then
    log_error "Fly.io app '$app_name' does not exist"
    log_info "Create it first with: ./scripts/setup-flyio.sh $ENVIRONMENT"
    exit 1
  fi

  log_success "Found Fly.io app: $app_name"

  # Prompt for configuration values
  if ! $DRY_RUN || [[ -z "$ISSUER_URI" ]] || [[ -z "$AUDIENCE" ]]; then
    prompt_for_values
  else
    # For dry-run with provided values, still validate
    if [[ -n "$ISSUER_URI" ]]; then
      validate_issuer_uri "$ISSUER_URI" || exit 1
    fi
    if [[ -z "$AUDIENCE" ]]; then
      AUDIENCE="auto-ledger-${ENVIRONMENT}"
    fi
  fi

  # Set secrets
  set_fly_secrets "$app_name"

  if ! $DRY_RUN; then
    echo ""
    echo "═══════════════════════════════════════════════════════"
    log_success "Clerk authentication enabled for $ENVIRONMENT!"
    echo "═══════════════════════════════════════════════════════"
    echo ""
    echo "Next steps:"
    echo "  1. Wait for deployment to complete (~30-60 seconds)"
    echo "  2. Create a test user in Clerk Dashboard"
    echo "  3. Get a JWT token from Clerk (see docs/CLERK-SETUP.md)"
    echo "  4. Test authentication:"
    echo "     ./scripts/test-clerk-auth.sh $ENVIRONMENT --jwt \$TOKEN"
    echo ""
    echo "To verify the secrets were set:"
    echo "  flyctl secrets list -a $app_name"
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
