#!/bin/bash
#
# Test Clerk authentication for Auto Ledger
#
# Usage:
#   ./scripts/test-clerk-auth.sh staging
#   ./scripts/test-clerk-auth.sh production
#   ./scripts/test-clerk-auth.sh staging --jwt "eyJhbGc..."
#
# Prerequisites:
#   - curl installed
#   - Auth enabled in the target environment
#   - Valid JWT token from Clerk (see docs/CLERK-SETUP.md)
#
# Tests performed:
#   1. Health endpoint (should be 200 without auth)
#   2. API endpoint without JWT (should be 401)
#   3. API endpoint with JWT (should be 200)
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
JWT_TOKEN=""

# Parse arguments
shift || true  # Skip first arg (environment)
while [[ $# -gt 0 ]]; do
  case $1 in
    --jwt)
      JWT_TOKEN="$2"
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

log_test() {
  echo -e "${BLUE}▶${NC} $1"
}

#######################################
# Validation functions
#######################################
validate_environment() {
  if [[ ! "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
    log_error "Invalid environment. Must be 'staging' or 'production'"
    echo "Usage: $0 <staging|production> [--jwt TOKEN]"
    exit 1
  fi
}

check_prerequisites() {
  # Check curl
  if ! command -v curl &> /dev/null; then
    log_error "curl not found. Install: sudo apt install curl (or brew install curl)"
    exit 1
  fi
}

validate_jwt_format() {
  local token="$1"

  # JWT should have 3 parts separated by dots
  local parts
  parts=$(echo "$token" | tr '.' '\n' | wc -l)

  if [[ $parts -ne 3 ]]; then
    log_error "Invalid JWT format. Expected 3 parts (header.payload.signature), got $parts"
    return 1
  fi

  # First part (header) should be base64url encoded JSON
  local header
  header=$(echo "$token" | cut -d'.' -f1)

  if [[ ${#header} -lt 10 ]]; then
    log_error "JWT header is too short"
    return 1
  fi

  return 0
}

prompt_for_jwt() {
  echo ""
  echo "════════════════════════════════════════════════════════════════"
  echo "  How to get a JWT token from Clerk:"
  echo "════════════════════════════════════════════════════════════════"
  echo ""
  echo "1. Go to Clerk Dashboard: https://dashboard.clerk.com"
  echo "2. Select your application"
  echo "3. Go to: Users → Select a user → Sessions"
  echo "4. Click on an active session or create one"
  echo "5. Copy the JWT token"
  echo ""
  echo "Or see: docs/CLERK-SETUP.md for detailed instructions"
  echo ""
  echo "════════════════════════════════════════════════════════════════"
  echo ""
  read -p "Paste your JWT token: " JWT_TOKEN
}

#######################################
# Test functions
#######################################
test_health_endpoint() {
  local url="$1"
  local test_name="Health endpoint (no auth required)"

  log_test "$test_name"

  local response
  local http_code

  response=$(curl -s -w "\n%{http_code}" "$url/actuator/health" 2>/dev/null)
  http_code=$(echo "$response" | tail -n1)

  if [[ "$http_code" == "200" ]]; then
    log_success "Health check passed (HTTP $http_code)"
    return 0
  else
    log_error "Health check failed (HTTP $http_code)"
    echo "Response: $response"
    return 1
  fi
}

test_api_without_auth() {
  local url="$1"
  local test_name="API endpoint without JWT (should be 401)"

  log_test "$test_name"

  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url/api/cars" 2>/dev/null)

  if [[ "$http_code" == "401" ]]; then
    log_success "Correctly rejected (HTTP $http_code)"
    return 0
  else
    log_error "Expected 401, got HTTP $http_code"
    log_warning "Authentication may not be enabled or endpoint is misconfigured"
    return 1
  fi
}

test_api_with_auth() {
  local url="$1"
  local token="$2"
  local test_name="API endpoint with valid JWT (should be 200)"

  log_test "$test_name"

  local response
  local http_code

  response=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $token" \
    "$url/api/cars" 2>/dev/null)

  http_code=$(echo "$response" | tail -n1)
  local body
  body=$(echo "$response" | head -n-1)

  if [[ "$http_code" == "200" ]]; then
    log_success "Authentication successful (HTTP $http_code)"

    # Check if response is valid JSON array
    if echo "$body" | jq -e 'type == "array"' >/dev/null 2>&1; then
      local count
      count=$(echo "$body" | jq 'length')
      log_info "Response: JSON array with $count item(s)"

      if [[ $count -eq 0 ]]; then
        log_info "No cars found (expected for new users)"
      else
        log_info "Found $count car(s) for this user"
      fi
    else
      log_warning "Response is not a JSON array (unexpected)"
      echo "Body: $body"
    fi

    return 0
  elif [[ "$http_code" == "401" ]]; then
    log_error "Authentication failed (HTTP $http_code)"
    log_warning "Possible causes:"
    echo "  • JWT is expired (check 'exp' claim)"
    echo "  • Issuer URI mismatch (check 'iss' claim vs JWT_ISSUER_URI)"
    echo "  • Audience mismatch (check 'aud' claim vs JWT_AUDIENCE)"
    echo "  • Invalid signature (wrong JWKS key)"
    echo ""
    echo "Debug your JWT at: https://jwt.io"
    echo "Response: $body"
    return 1
  else
    log_error "Unexpected response (HTTP $http_code)"
    echo "Response: $body"
    return 1
  fi
}

#######################################
# Main execution
#######################################
main() {
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  Auto Ledger - Clerk Authentication Test"
  echo "  Environment: $ENVIRONMENT"
  echo "═══════════════════════════════════════════════════════"
  echo ""

  # Validate inputs
  validate_environment
  check_prerequisites

  # Determine app URL
  local app_url
  if [[ "$ENVIRONMENT" == "staging" ]]; then
    app_url="https://auto-ledger-staging.fly.dev"
  else
    app_url="https://auto-ledger-prod.fly.dev"
  fi

  log_info "Target URL: $app_url"
  echo ""

  # Prompt for JWT if not provided
  if [[ -z "$JWT_TOKEN" ]]; then
    prompt_for_jwt
  fi

  # Validate JWT format
  if ! validate_jwt_format "$JWT_TOKEN"; then
    log_error "Invalid JWT token format"
    exit 1
  fi

  log_success "JWT format validated"
  echo ""

  # Run tests
  echo "════════════════════════════════════════════════════════"
  echo "  Running Tests..."
  echo "════════════════════════════════════════════════════════"
  echo ""

  local test_passed=0
  local test_failed=0

  # Test 1: Health endpoint
  if test_health_endpoint "$app_url"; then
    ((test_passed++))
  else
    ((test_failed++))
  fi
  echo ""

  # Test 2: API without auth
  if test_api_without_auth "$app_url"; then
    ((test_passed++))
  else
    ((test_failed++))
  fi
  echo ""

  # Test 3: API with auth
  if test_api_with_auth "$app_url" "$JWT_TOKEN"; then
    ((test_passed++))
  else
    ((test_failed++))
  fi
  echo ""

  # Summary
  echo "════════════════════════════════════════════════════════"
  echo "  Test Summary"
  echo "════════════════════════════════════════════════════════"
  echo ""
  echo "  Passed: $test_passed"
  echo "  Failed: $test_failed"
  echo ""

  if [[ $test_failed -eq 0 ]]; then
    log_success "All tests passed! Clerk authentication is working correctly."
    echo ""
    echo "Next steps:"
    echo "  • Authentication is working in $ENVIRONMENT"
    echo "  • JIT user provisioning created a user record"
    echo "  • Ready for frontend integration"
    echo ""
    exit 0
  else
    log_error "Some tests failed. Please check the errors above."
    echo ""
    echo "Troubleshooting:"
    echo "  1. Verify secrets are set: flyctl secrets list -a auto-ledger-$ENVIRONMENT"
    echo "  2. Check backend logs: fly logs -a auto-ledger-$ENVIRONMENT"
    echo "  3. Decode JWT at: https://jwt.io"
    echo "  4. See: docs/CLERK-SETUP.md (Troubleshooting section)"
    echo ""
    exit 1
  fi
}

# Run main function
main
