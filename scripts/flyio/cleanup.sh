#!/bin/bash
#
# Cleanup Fly.io application for Auto Ledger
#
# Usage:
#   ./scripts/cleanup-flyio.sh staging [--dry-run] [--force]
#   ./scripts/cleanup-flyio.sh production [--dry-run] [--force]
#
# Prerequisites:
#   - Fly CLI installed: brew install flyctl
#   - Logged in: fly auth login
#   - GitHub CLI installed: gh auth login
#
# Safety features:
#   - Requires explicit confirmation before deletion
#   - Supports dry-run mode to preview deletions
#   - Checks if app exists before attempting deletion (idempotent)
#   - Production environment requires --force flag
#

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ENVIRONMENT="${1:-}"
DRY_RUN=false
FORCE=false

for arg in "$@"; do
  case $arg in
    --dry-run) DRY_RUN=true; shift ;;
    --force) FORCE=true; shift ;;
  esac
done

log_info() { echo -e "${BLUE}ℹ${NC} $1"; }
log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
log_error() { echo -e "${RED}✗${NC} $1"; }
log_dry_run() { echo -e "${YELLOW}[DRY RUN]${NC} $1"; }

validate_environment() {
  if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
    log_error "Invalid environment. Must be 'staging' or 'production'"
    exit 1
  fi

  if [[ "$ENVIRONMENT" == "production" ]] && ! $FORCE; then
    log_error "Deleting PRODUCTION requires --force flag"
    exit 1
  fi
}

check_prerequisites() {
  log_info "Checking prerequisites..."
  
  if ! command -v flyctl &> /dev/null && ! command -v fly &> /dev/null; then
    log_error "Fly CLI not found. Install: brew install flyctl"
    exit 1
  fi

  if ! flyctl auth whoami &> /dev/null; then
    log_error "Not logged in to Fly.io. Run: fly auth login"
    exit 1
  fi

  log_success "All prerequisites met"
}

app_exists() {
  flyctl apps list 2>/dev/null | grep -q "^$1"
}

delete_flyio_app() {
  local app_name="$1"

  if $DRY_RUN; then
    log_dry_run "Would delete Fly.io app: $app_name"
    return 0
  fi

  log_info "Deleting Fly.io app: $app_name..."

  if ! flyctl apps destroy "$app_name" --yes 2>&1; then
    log_error "Failed to delete Fly.io app"
    return 1
  fi

  log_success "App deleted successfully"
}

main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Fly.io Application Cleanup"
  echo "  Environment: $ENVIRONMENT"
  if $DRY_RUN; then
    echo "  Mode: DRY RUN (no changes will be made)"
  fi
  if [[ "$ENVIRONMENT" == "production" ]]; then
    echo -e "  ${RED}WARNING: PRODUCTION ENVIRONMENT${NC}"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""

  validate_environment
  check_prerequisites

  local app_name="auto-ledger-${ENVIRONMENT}"

  log_info "Checking current state..."
  echo ""

  if app_exists "$app_name"; then
    echo "Fly.io App:"
    echo "  Name: $app_name"
    echo "  Hostname: ${app_name}.fly.dev"
  else
    echo "Fly.io App:"
    echo "  Status: Not found (already deleted or never created)"
    log_success "Environment already clean - nothing to delete"
    exit 0
  fi

  echo ""
  echo "The following resources will be deleted:"
  echo ""
  echo "  • Fly.io app: $app_name"
  echo "    - All machines and volumes will be permanently deleted"
  echo "    - This action cannot be undone"
  echo ""

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
      log_info "Aborted by user"
      exit 0
    fi
  fi

  delete_flyio_app "$app_name"

  echo ""
  echo "═══════════════════════════════════════════════════════"
  if $DRY_RUN; then
    log_info "Dry run complete. No changes were made."
  else
    log_success "Cleanup complete for $ENVIRONMENT environment"
  fi
  echo "═══════════════════════════════════════════════════════"
  echo ""
}

main
