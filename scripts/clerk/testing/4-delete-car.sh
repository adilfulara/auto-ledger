#!/bin/bash
#
# Test DELETE /api/cars/{id} with Clerk JWT
#
# Usage:
#   ./scripts/clerk/testing/4-delete-car.sh [staging] [car-id]
#
# Examples:
#   ./scripts/clerk/testing/4-delete-car.sh staging
#   ./scripts/clerk/testing/4-delete-car.sh staging abc-123-def
#
# Prerequisites:
#   - Run ./scripts/clerk/testing/1-issue-jwt.sh first
#   - Or set JWT environment variable
#

set -euo pipefail

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ENVIRONMENT="${1:-staging}"
CAR_ID="${2:-}"

# Determine URL
if [ "$ENVIRONMENT" == "production" ]; then
  API_URL="https://auto-ledger-prod.fly.dev"
else
  API_URL="https://auto-ledger-staging.fly.dev"
fi

echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Test: DELETE /api/cars/{id}${NC}"
echo -e "${BLUE}  Environment: $ENVIRONMENT${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
echo ""

# Check for JWT
if [ -z "${JWT:-}" ]; then
  if [ -f /tmp/clerk_jwt.txt ]; then
    JWT=$(cat /tmp/clerk_jwt.txt)
    echo -e "${BLUE}ℹ${NC} Using JWT from /tmp/clerk_jwt.txt"
  else
    echo -e "${YELLOW}⚠${NC} No JWT found"
    echo ""
    echo "Run this first:"
    echo "  ./scripts/clerk/testing/1-issue-jwt.sh"
    exit 1
  fi
fi

echo ""

# Get car ID if not provided
if [ -z "$CAR_ID" ]; then
  echo -e "${BLUE}➤${NC} Fetching your cars..."
  CARS_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X GET "$API_URL/api/cars" \
    -H "Authorization: Bearer $JWT")

  CARS_HTTP_CODE=$(echo "$CARS_RESPONSE" | tail -n1)
  CARS_BODY=$(echo "$CARS_RESPONSE" | head -n-1)

  if [ "$CARS_HTTP_CODE" == "200" ]; then
    CAR_COUNT=$(echo "$CARS_BODY" | jq '. | length')

    if [ "$CAR_COUNT" == "0" ]; then
      echo -e "${YELLOW}⚠${NC} No cars found"
      echo ""
      echo "Create a car first:"
      echo "  ./scripts/clerk/testing/3-create-car.sh staging"
      exit 0
    fi

    echo -e "${GREEN}✓${NC} Found $CAR_COUNT car(s)"
    echo ""
    echo -e "${BLUE}Your cars:${NC}"
    echo "$CARS_BODY" | jq -r '.[] | "\(.id) - \(.year) \(.make) \(.model) (\(.name))"'
    echo ""

    # Prompt for car ID
    echo -e "${BLUE}➤${NC} Enter car ID to delete:"
    read -r CAR_ID

    if [ -z "$CAR_ID" ]; then
      echo -e "${RED}✗${NC} No car ID provided"
      exit 1
    fi
  else
    echo -e "${RED}✗${NC} Failed to fetch cars (HTTP $CARS_HTTP_CODE)"
    exit 1
  fi
fi

echo ""
echo -e "${BLUE}➤${NC} Deleting car: $CAR_ID..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X DELETE "$API_URL/api/cars/$CAR_ID" \
  -H "Authorization: Bearer $JWT")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

if [ "$HTTP_CODE" == "204" ] || [ "$HTTP_CODE" == "200" ]; then
  echo -e "${GREEN}✓${NC} Car deleted successfully (HTTP $HTTP_CODE)"
  echo ""

  # Verify deletion
  echo -e "${BLUE}➤${NC} Verifying deletion..."
  VERIFY_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X GET "$API_URL/api/cars/$CAR_ID" \
    -H "Authorization: Bearer $JWT")

  VERIFY_HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -n1)

  if [ "$VERIFY_HTTP_CODE" == "404" ]; then
    echo -e "${GREEN}✓${NC} Car no longer exists (HTTP 404)"
  else
    echo -e "${YELLOW}⚠${NC} Unexpected verification result (HTTP $VERIFY_HTTP_CODE)"
  fi
elif [ "$HTTP_CODE" == "404" ]; then
  echo -e "${YELLOW}⚠${NC} Car not found (HTTP $HTTP_CODE)"
  echo ""
  echo "Possible reasons:"
  echo "  - Car ID is incorrect"
  echo "  - Car was already deleted"
  echo "  - Car belongs to a different user"
elif [ "$HTTP_CODE" == "401" ]; then
  echo -e "${RED}✗${NC} Authentication failed (HTTP $HTTP_CODE)"
  echo ""
  echo "JWT might be expired. Generate a fresh one:"
  echo "  ./scripts/clerk/testing/1-issue-jwt.sh"
else
  echo -e "${RED}✗${NC} Failed to delete car (HTTP $HTTP_CODE)"
  echo ""
  if [ -n "$BODY" ]; then
    echo -e "${YELLOW}Response:${NC}"
    echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
  fi
fi
echo ""
